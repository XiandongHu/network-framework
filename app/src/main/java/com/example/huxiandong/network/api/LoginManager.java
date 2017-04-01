package com.example.huxiandong.network.api;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;

import com.example.huxiandong.network.api.exception.NeedUserInteractionException;
import com.example.huxiandong.network.api.logger.ApiLogger;
import com.xiaomi.accountsdk.account.data.ExtendedAuthToken;
import com.xiaomi.passport.accountmanager.MiAccountManager;
import com.xiaomi.passport.data.XMPassportInfo;
import com.xiaomi.passport.servicetoken.ServiceTokenResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by huxiandong
 * on 17/3/4.
 */

public class LoginManager implements CookieJar, ApiResponseHandler {

    @SuppressLint("StaticFieldLeak")
    private static LoginManager sInstance;

    static LoginManager init(Context context, Scheduler scheduler, Handler handler, ApiLogger apiLogger) {
        if (sInstance != null) {
            throw new IllegalStateException("LoginManager has already been initialized.");
        }
        sInstance = new LoginManager(context, scheduler, handler, apiLogger);
        return sInstance;
    }

    public static LoginManager getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("LoginManager has not been initialized.");
        }
        return sInstance;
    }

    private final Context mContext;
    private final Scheduler mScheduler;
    private final Handler mHandler;
    private final ApiLogger mApiLogger;

    private final MiAccountManager mAccountManager;
    private final AccountStore mAccountStore;
    private AccountInfo mAccountInfo;

    private Set<CacheCookie> mCacheCookies = new HashSet<>();
    private final Object mCookieLock = new Object();

    private List<ApiRequest> mPendingRequests = new ArrayList<>();
    private final Object mRequestLock = new Object();

    private enum State {
        INITIALIZED,
        VALID,
        REFRESHING,
        LOGIN_REQUIRED
    }

    private static class ServiceTokenStatus {
        private State state;
        private long lastRefreshTimeStamp;

        private ServiceTokenStatus() {
            state = State.INITIALIZED;
            lastRefreshTimeStamp = 0L;
        }
    }

    private final ServiceTokenStatus mServiceTokenStatus = new ServiceTokenStatus();

    private LoginManager(Context context, Scheduler scheduler, Handler handler, ApiLogger apiLogger) {
        mContext = context;
        mScheduler = scheduler;
        mHandler = handler;
        mApiLogger = apiLogger;

        mAccountManager = MiAccountManager.get(context);
        mAccountStore = new SharedPrefsStore(context);
        loadAccountInfo();
    }

    private synchronized void loadAccountInfo() {
        if (mAccountInfo != null) {
            throw new IllegalStateException("Account info has been loaded.");
        }

        mAccountInfo = AccountInfo.load(mAccountManager, mAccountStore);
        if (mAccountInfo.isValid()) {
            addOrUpdateCookies(ApiConstants.MICO_SID);
            updateServiceTokenStatus(ApiConstants.MICO_SID, State.VALID, 0L);
        }
    }

    private void addOrUpdateCookies(String sid) {
        if (mAccountInfo.isValid()) {
            HttpUrl httpUrl = HttpUrl.parse(ApiConstants.sidToUrl.get(sid));
            List<Cookie> cookies = new ArrayList<>(2);
            Cookie.Builder builder = new Cookie.Builder()
                    .secure()
                    .domain(httpUrl.host())
                    .path("/");
            Cookie userIdCookie = builder.name("userId")
                    .value(mAccountInfo.getPassportInfo().getUserId())
                    .build();
            Cookie cUserIdCookie = builder.name("cUserId")
                    .value(mAccountInfo.getPassportInfo().getCUserId())
                    .build();
            Cookie serviceTokenCookie = builder.name("serviceToken")
                    .value(mAccountInfo.getServiceInfo(sid).getServiceToken())
                    .build();
            cookies.add(userIdCookie);
            cookies.add(cUserIdCookie);
            cookies.add(serviceTokenCookie);
            saveFromResponse(httpUrl, cookies);
        }
    }

    private void updateServiceTokenStatus(String sid, State state, long lastRefreshTimeStamp) {
        if (ApiConstants.MICO_SID.equals(sid)) {
            mServiceTokenStatus.state = state;
            mServiceTokenStatus.lastRefreshTimeStamp = lastRefreshTimeStamp;
        }
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        synchronized (mCookieLock) {
            if (mCacheCookies == null || mCacheCookies.isEmpty()) {
                return Collections.emptyList();
            }

            List<Cookie> validCookies = new ArrayList<>();
            Iterator<CacheCookie> it = mCacheCookies.iterator();
            while (it.hasNext()) {
                Cookie cookie = it.next().getCookie();
                if (cookie.expiresAt() < System.currentTimeMillis()) {
                    it.remove();
                } else if (cookie.matches(url)) {
                    validCookies.add(cookie);
                }
            }

            return validCookies;
        }
    }

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        synchronized (mCookieLock) {
            for (CacheCookie cacheCookie : CacheCookie.decorateCookies(cookies)) {
                mCacheCookies.remove(cacheCookie);
                mCacheCookies.add(cacheCookie);
            }
        }
    }

    @Override
    public Scheduler getScheduler() {
        return mScheduler;
    }

    @Override
    public void retry(ApiRequest apiRequest) {
        mApiLogger.d("Retry request.");
        if (mServiceTokenStatus.state == State.VALID) {
            long timeGap = Math.abs(System.currentTimeMillis() - mServiceTokenStatus.lastRefreshTimeStamp);
            if (TimeUnit.MILLISECONDS.toSeconds(timeGap) > 20) {
                synchronized (mRequestLock) {
                    mPendingRequests.add(apiRequest);
                }
                mApiLogger.d("Refresh mico service token.");
                updateServiceTokenStatus(ApiConstants.MICO_SID, State.REFRESHING, System.currentTimeMillis());
                refreshServiceToken(ApiConstants.MICO_SID)
                        .subscribe(new Action1<Boolean>() {
                            @Override
                            public void call(Boolean success) {
                                if (success) {
                                    mApiLogger.d("Refresh mico service token success.");
                                    executePendingRequests();
                                } else {
                                    mApiLogger.d("Refresh mico service token failed.");
                                    mServiceTokenStatus.state = State.LOGIN_REQUIRED;
                                    updateServiceTokenStatus(ApiConstants.MICO_SID, State.LOGIN_REQUIRED, System.currentTimeMillis());
                                }
                            }
                        });
            } else {
                executeRequest(apiRequest);
            }
        } else if (mServiceTokenStatus.state == State.REFRESHING) {
            synchronized (mRequestLock) {
                mPendingRequests.add(apiRequest);
            }
        } else {
            apiRequest.error(ApiError.TOKEN_INSUFFICIENT);
        }
    }

    @Override
    public void cancel(ApiRequest apiRequest) {
        synchronized (mRequestLock) {
            mPendingRequests.remove(apiRequest);
        }
    }

    private void executePendingRequests() {
        synchronized (mRequestLock) {
            Iterator it = mPendingRequests.iterator();
            while (it.hasNext()) {
                ApiRequest apiRequest = (ApiRequest) it.next();
                executeRequest(apiRequest);
                it.remove();
            }
        }
    }

    private void executeRequest(ApiRequest apiRequest) {
        if (!apiRequest.isCanceled()) {
            apiRequest.subscribe();
        }
    }

    public AccountInfo.PassportInfo getPassportInfo() {
        return mAccountInfo.getPassportInfo();
    }

    public AccountInfo.ServiceInfo getServiceInfo(String sid) {
        return mAccountInfo.getServiceInfo(sid);
    }

    public boolean hasValidAccount() {
        return mAccountInfo.isValid();
    }

    AccountType getAccountType() {
        return mAccountStore.getAccountType();
    }

    public boolean hasSystemAccount() {
        return mAccountManager.canUseSystem();
    }

    public String getSystemAccountUserId() {
        if (hasSystemAccount()) {
            AccountManager am = AccountManager.get(mContext);
            Account[] accounts = am.getAccountsByType(MiAccountManager.XIAOMI_ACCOUNT_TYPE);
            if (accounts.length > 0) {
                return accounts[0].name;
            }
        }
        return null;
    }

    public Observable<LoginState> loginBySystemAccount(final Activity activity) {
        if (activity == null) {
            throw new IllegalStateException("Activity must be provided when login.");
        }

        mAccountManager.setUseSystem();
        final String sid = ApiConstants.MICO_SID;
        return Observable.create(new Observable.OnSubscribe<XMPassportInfo>() {
            @Override
            public void call(Subscriber<? super XMPassportInfo> subscriber) {
                if (subscriber.isUnsubscribed()) {
                    return;
                }

                ServiceTokenResult serviceTokenResult = mAccountManager.getServiceToken(activity, sid).get();
                if (serviceTokenResult.errorCode == ServiceTokenResult.ErrorCode.ERROR_USER_INTERACTION_NEEDED
                        && serviceTokenResult.intent != null) {
                    subscriber.onError(new NeedUserInteractionException(serviceTokenResult.intent));
                    return;
                }

                subscriber.onNext(XMPassportInfo.build(mContext, sid));
                subscriber.onCompleted();
            }
        })
                .subscribeOn(Schedulers.newThread())
                .observeOn(mScheduler)
                .map(new Func1<XMPassportInfo, LoginState>() {
                    @Override
                    public LoginState call(XMPassportInfo xmPassportInfo) {
                        LoginState state;
                        Account account = mAccountManager.getXiaomiAccount();
                        if (account == null) {
                            state = LoginState.NO_ACCOUNT;
                        } else if (xmPassportInfo != null) {
                            state = LoginState.SUCCESS;
                            String cUserId = xmPassportInfo.getEncryptedUserId();
                            String authToken = ExtendedAuthToken.build(xmPassportInfo.getServiceToken(),
                                    xmPassportInfo.getSecurity()).toPlain();
                            mAccountInfo.updateAccountCoreInfo(mAccountManager, mAccountStore,
                                    account, cUserId, authToken);
                            addOrUpdateCookies(sid);
                            updateServiceTokenStatus(sid, State.VALID, System.currentTimeMillis());
                        } else {
                            state = LoginState.FAILED;
                        }
                        return state;
                    }
                });
    }

    public Observable<LoginState> loginByLocalAccount(final Activity activity) {
        if (activity == null) {
            throw new IllegalStateException("Activity must be provided when login.");
        }

        mAccountManager.setUseLocal();
        final String sid = ApiConstants.MICO_SID;
        return Observable.create(new Observable.OnSubscribe<LoginState>() {
            @Override
            public void call(final Subscriber<? super LoginState> subscriber) {
                mAccountManager.addXiaomiAccount(sid, activity, new AccountManagerCallback<Bundle>() {
                    @Override
                    public void run(AccountManagerFuture<Bundle> future) {
                        if (subscriber.isUnsubscribed()) {
                            return;
                        }

                        LoginState state;
                        try {
                            Bundle result = future.getResult();
                            if (result.getBoolean(MiAccountManager.KEY_BOOLEAN_RESULT)) {
                                state = LoginState.SUCCESS;
                                Account account = mAccountManager.getXiaomiAccount();
                                String cUserId = mAccountManager.getUserData(account, "encrypted_user_id");
                                String authToken = mAccountManager.peekAuthToken(account, sid);
                                mAccountInfo.updateAccountCoreInfo(mAccountManager, mAccountStore,
                                        account, cUserId, authToken);
                                addOrUpdateCookies(sid);
                                updateServiceTokenStatus(sid, State.VALID, System.currentTimeMillis());
                            } else {
                                state = LoginState.FAILED;
                                mApiLogger.w("Add account failed or canceled.");
                            }
                        } catch (OperationCanceledException | AuthenticatorException | IOException e) {
                            state = LoginState.FAILED;
                            mApiLogger.e("Add account exception: %s.", e);
                        }
                        subscriber.onNext(state);
                        subscriber.onCompleted();
                    }
                }, mHandler);
            }
        });
    }

    private Observable<Boolean> refreshServiceToken(final String sid) {
        return Observable.create(new Observable.OnSubscribe<ServiceTokenResult>() {
            @Override
            public void call(Subscriber<? super ServiceTokenResult> subscriber) {
                if (subscriber.isUnsubscribed()) {
                    return;
                }

                ServiceTokenResult serviceTokenResult = mAccountManager.getServiceToken(mContext, sid).get();
                if (serviceTokenResult.errorCode == ServiceTokenResult.ErrorCode.ERROR_NONE) {
                    mAccountManager.invalidateServiceToken(mContext, serviceTokenResult).get();
                }
                subscriber.onNext(mAccountManager.getServiceToken(mContext, sid).get());
                subscriber.onCompleted();
            }
        })
                .subscribeOn(Schedulers.newThread())
                .observeOn(mScheduler)
                .map(new Func1<ServiceTokenResult, Boolean>() {
                    @Override
                    public Boolean call(ServiceTokenResult serviceTokenResult) {
                        if (serviceTokenResult.errorCode == ServiceTokenResult.ErrorCode.ERROR_NONE) {
                            mAccountInfo.updateServiceInfo(mAccountStore, sid, serviceTokenResult.serviceToken,
                                    serviceTokenResult.security);
                            addOrUpdateCookies(sid);
                            updateServiceTokenStatus(sid, State.VALID, System.currentTimeMillis());
                            return true;
                        } else {
                            mApiLogger.e("Refresh service token failed: %s.", serviceTokenResult.errorMessage);
                            return false;
                        }
                    }
                });
    }

    public Observable<LoginState> logout() {
        return Observable.create(new Observable.OnSubscribe<LoginState>() {
            @Override
            public void call(final Subscriber<? super LoginState> subscriber) {
                Account account = mAccountManager.getXiaomiAccount();
                if (account == null) {
                    onAccountRemoved();
                    subscriber.onNext(LoginState.NO_ACCOUNT);
                    subscriber.onCompleted();
                    return;
                }

                if (mAccountManager.isUseLocal()) {
                    mAccountManager.removeAccount(account, new AccountManagerCallback<Boolean>() {
                        @Override
                        public void run(AccountManagerFuture<Boolean> future) {
                            if (subscriber.isUnsubscribed()) {
                                return;
                            }

                            LoginState state;
                            try {
                                Boolean result = future.getResult();
                                if (result) {
                                    onAccountRemoved();
                                    state = LoginState.SUCCESS;
                                } else {
                                    state = LoginState.FAILED;
                                }
                            } catch (OperationCanceledException | AuthenticatorException | IOException e) {
                                state = LoginState.FAILED;
                            }
                            subscriber.onNext(state);
                            subscriber.onCompleted();
                        }
                    }, mHandler);
                } else {
                    onAccountRemoved();
                    subscriber.onNext(LoginState.SUCCESS);
                    subscriber.onCompleted();
                }
            }
        })
                .subscribeOn(mScheduler);
    }

    private void onAccountRemoved() {
        mAccountInfo.remove(mAccountStore);
        synchronized (mCookieLock) {
            mCacheCookies.clear();
        }
        updateServiceTokenStatus(ApiConstants.MICO_SID, State.INITIALIZED, System.currentTimeMillis());
    }

}
