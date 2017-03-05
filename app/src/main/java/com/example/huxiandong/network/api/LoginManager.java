package com.example.huxiandong.network.api;

import android.accounts.Account;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;

import com.example.huxiandong.network.api.logger.ApiLogger;
import com.xiaomi.passport.accountmanager.MiAccountManager;
import com.xiaomi.passport.servicetoken.ServiceTokenResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by huxiandong
 * on 17/3/4.
 */

public class LoginManager implements CookieJar {

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
    private final LoginSettings mLoginSettings;
    private AccountInfo mAccountInfo;

    private ConcurrentHashMap<HttpUrl, Set<CacheCookie>> mCacheCookieMap = new ConcurrentHashMap<>();
    private final Object mCookieLock = new Object();

    private LoginManager(Context context, Scheduler scheduler, Handler handler, ApiLogger apiLogger) {
        mContext = context;
        mScheduler = scheduler;
        mHandler = handler;
        mApiLogger = apiLogger;

        mAccountManager = MiAccountManager.get(context);
        mLoginSettings = new LoginSettings(context);
        loadAccountInfo();
    }

    private synchronized void loadAccountInfo() {
        if (mAccountInfo != null) {
            throw new IllegalStateException("Account info has been loaded.");
        }

        mAccountInfo = AccountInfo.load(mAccountManager);
        if (mAccountInfo.isValid()) {
            addOrUpdateCookies(ApiConstants.MICO_SID);
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
            Cookie userIdCookie = builder.name("cUserId")
                    .value(mAccountInfo.getPassportInfo().getCUserId())
                    .build();
            Cookie serviceTokenCookie = builder.name("serviceToken")
                    .value(mAccountInfo.getServiceInfo(sid).getServiceToken())
                    .build();
            cookies.add(userIdCookie);
            cookies.add(serviceTokenCookie);
            saveFromResponse(httpUrl, cookies);
        }
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        synchronized (mCookieLock) {
            Set<CacheCookie> cacheCookies = mCacheCookieMap.get(url);
            if (cacheCookies == null) {
                return Collections.emptyList();
            }

            List<Cookie> validCookies = new ArrayList<>();
            Iterator<CacheCookie> it = cacheCookies.iterator();
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
            Set<CacheCookie> cacheCookies = mCacheCookieMap.get(url);
            if (cacheCookies == null) {
                cacheCookies = new HashSet<>();
                mCacheCookieMap.put(url, cacheCookies);
            }
            for (CacheCookie cacheCookie : CacheCookie.decorateCookies(cookies)) {
                cacheCookies.remove(cacheCookie);
                cacheCookies.add(cacheCookie);
            }
        }
    }

    public AccountInfo.PassportInfo getPassportInfo() {
        return mAccountInfo.getPassportInfo();
    }

    public AccountInfo.ServiceInfo getServiceInfo(String sid) {
        return mAccountInfo.getServiceInfo(sid);
    }

    LoginSettings.Mode getLoginMode() {
        return mLoginSettings.getMode();
    }

    public Observable<LoginState> loginBySystemAccount(final Activity activity) {
        mLoginSettings.setMode(LoginSettings.Mode.SYSTEM);
        mAccountManager.setUseSystem();
        return getAuthToken(activity);
    }

    public Observable<LoginState> loginByLocalAccount(final Activity activity) {
        mLoginSettings.setMode(LoginSettings.Mode.LOCAL);
        mAccountManager.setUseLocal();
        return addAccount(activity);
    }

    public Observable<LoginState> logout() {
        return removeAccount();
    }

    private Observable<LoginState> addAccount(final Activity activity) {
        if (activity == null) {
            throw new IllegalStateException("Activity must be provided when add account.");
        }

        return Observable.create(new Observable.OnSubscribe<LoginState>() {
            @Override
            public void call(final Subscriber<? super LoginState> subscriber) {
                mAccountManager.addXiaomiAccount(ApiConstants.MICO_SID, activity, new AccountManagerCallback<Bundle>() {
                    @Override
                    public void run(AccountManagerFuture<Bundle> future) {
                        if (subscriber.isUnsubscribed()) {
                            return;
                        }

                        LoginState state;
                        try {
                            Bundle bundle = future.getResult();
                            if (bundle.getBoolean(MiAccountManager.KEY_BOOLEAN_RESULT)) {
                                state = LoginState.SUCCESS;
                                // TODO: update account info and cookies
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

    private Observable<LoginState> getAuthToken(final Activity activity) {
        if (activity == null) {
            throw new IllegalStateException("Activity must be provided when get auth token.");
        }

        return Observable.create(new Observable.OnSubscribe<LoginState>() {
            @Override
            public void call(final Subscriber<? super LoginState> subscriber) {
                Account account = mAccountManager.getXiaomiAccount();
                if (account == null) {
                    subscriber.onNext(LoginState.NO_ACCOUNT);
                    subscriber.onCompleted();
                    return;
                }

                final String sid = ApiConstants.MICO_SID;
                invalidateAuthToken(sid);
                mAccountManager.getAuthToken(account, sid, null, activity, new AccountManagerCallback<Bundle>() {
                    @Override
                    public void run(AccountManagerFuture<Bundle> accountManagerFuture) {
                        if (subscriber.isUnsubscribed()) {
                            return;
                        }

                        LoginState state;
                        try {
                            Bundle bundle = accountManagerFuture.getResult();
                            String authToken = bundle.getString(MiAccountManager.KEY_AUTHTOKEN);
                            if (!TextUtils.isEmpty(authToken)) {
                                state = LoginState.SUCCESS;
                                // TODO: update account info and cookies
                            } else {
                                state = LoginState.FAILED;
                                mApiLogger.w("Get auth token account failed.");
                            }
                        } catch (OperationCanceledException | AuthenticatorException | IOException e) {
                            state = LoginState.FAILED;
                            mApiLogger.e("Get auth token exception: %s.", e);
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

                invalidateAuthToken(sid);
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
                            mAccountInfo.updateServiceInfo(sid, serviceTokenResult.serviceToken, serviceTokenResult.security);
                            addOrUpdateCookies(sid);
                            return true;
                        } else {
                            mApiLogger.e("Refresh service token failed: %s.", serviceTokenResult.errorMessage);
                            return false;
                        }
                    }
                });
    }

    private void invalidateAuthToken(String sid) {
        Account account = mAccountManager.getXiaomiAccount();
        if (account != null) {
            String authToken = mAccountManager.peekAuthToken(account, sid);
            if (!TextUtils.isEmpty(authToken)) {
                mAccountManager.invalidateAuthToken(sid, authToken);
            }
        }
    }

    private Observable<LoginState> removeAccount() {
        return Observable.create(new Observable.OnSubscribe<LoginState>() {
            @Override
            public void call(final Subscriber<? super LoginState> subscriber) {
                Account account = mAccountManager.getXiaomiAccount();
                if (account == null) {
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
                                Boolean success = future.getResult();
                                state = success ? LoginState.SUCCESS : LoginState.FAILED;
                            } catch (OperationCanceledException | AuthenticatorException | IOException e) {
                                state = LoginState.FAILED;
                            }
                            subscriber.onNext(state);
                            subscriber.onCompleted();
                        }
                    }, mHandler);
                } else {
                    subscriber.onNext(LoginState.SUCCESS);
                    subscriber.onCompleted();
                }
            }
        });
    }

    private void onAccountRemoved() {
        mAccountInfo.remove();
        synchronized (mCookieLock) {
            mCacheCookieMap.clear();
        }
        mLoginSettings.setMode(LoginSettings.Mode.NONE);
    }

}
