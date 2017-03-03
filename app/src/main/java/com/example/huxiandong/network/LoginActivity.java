package com.example.huxiandong.network;

import android.accounts.Account;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.xiaomi.accounts.Manifest;
import com.xiaomi.accountsdk.account.PassportCATokenManager;
import com.xiaomi.accountsdk.account.data.XiaomiUserCoreInfo;
import com.xiaomi.accountsdk.account.exception.IllegalDeviceException;
import com.xiaomi.accountsdk.account.exception.InvalidCredentialException;
import com.xiaomi.accountsdk.account.exception.InvalidUserNameException;
import com.xiaomi.accountsdk.request.AccessDeniedException;
import com.xiaomi.accountsdk.request.AuthenticationFailureException;
import com.xiaomi.accountsdk.request.CipherException;
import com.xiaomi.accountsdk.request.InvalidResponseException;
import com.xiaomi.accountsdk.utils.AccountLog;
import com.xiaomi.passport.Constants;
import com.xiaomi.passport.LocalFeatures.LocalFeaturesManagerCallback;
import com.xiaomi.passport.LocalFeatures.LocalFeaturesManagerFuture;
import com.xiaomi.passport.LocalFeatures.MiLocalFeaturesManager;
import com.xiaomi.passport.PassportCAExternalImpl;
import com.xiaomi.passport.accountmanager.MiAccountManager;
import com.xiaomi.passport.servicetoken.ServiceTokenResult;
import com.xiaomi.passport.utils.AccountHelper;
import com.xiaomi.passport.utils.CUserIdUtil;
import com.xiaomi.passport.utils.RuntimePermissionActor;
import com.xiaomi.passport.utils.RuntimePermissionHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;

/**
 * Created by huxiandong
 * on 17-3-3.
 */

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String PASSPORT_API_SID = "passportapi";
    private static final String TAG = "LoginActivity";
    private MiAccountManager mAccountManager;

    private enum STAGE {
        INITIAL,
        SYSTEM_ACCOUNT,
        LOCAL_ACCOUNT;
    }

    private static EnumMap<STAGE, HashMap<Integer, Integer>> visibility = new EnumMap<STAGE, HashMap<Integer, Integer>>(STAGE.class);
    private static List<Integer> noAccountDisplayId = new ArrayList<Integer>();
    private static List<Integer> hasAccountDisplayId = new ArrayList<Integer>();

    static {
        //there is no account(neither system nor local account)
        noAccountDisplayId.add(R.id.login_system_btn);
        noAccountDisplayId.add(R.id.login_local_btn);

        //a account has logined (either system or local account)
        hasAccountDisplayId.add(R.id.account_id);
        hasAccountDisplayId.add(R.id.confirm_btn);
        hasAccountDisplayId.add(R.id.remove_btn);
        hasAccountDisplayId.add(R.id.authToken);
        hasAccountDisplayId.add(R.id.serviceToken);
        hasAccountDisplayId.add(R.id.stsUrl);
        hasAccountDisplayId.add(R.id.cUserId);
        hasAccountDisplayId.add(R.id.getCoreInfo);
        hasAccountDisplayId.add(R.id.scan_barcode);

        //initial state
        HashMap<Integer, Integer> initialStatMap = new HashMap<>();
        //system account login
        HashMap<Integer, Integer> systemAccountMap = new HashMap<>();
        //local account login
        HashMap<Integer, Integer> localAccountMap = new HashMap<>();
        for (Integer resId : noAccountDisplayId) {
            initialStatMap.put(resId, View.VISIBLE);
            systemAccountMap.put(resId, View.GONE);
            localAccountMap.put(resId, View.GONE);
        }
        for (Integer resId : hasAccountDisplayId) {
            initialStatMap.put(resId, View.GONE);
            systemAccountMap.put(resId, View.VISIBLE);
            localAccountMap.put(resId, View.VISIBLE);
        }

        visibility.put(STAGE.INITIAL, initialStatMap);
        visibility.put(STAGE.SYSTEM_ACCOUNT, systemAccountMap);
        visibility.put(STAGE.LOCAL_ACCOUNT, localAccountMap);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        //make each view can clickable
        for (Integer item : noAccountDisplayId) {
            findViewById(item).setOnClickListener(this);
        }
        for (Integer item : hasAccountDisplayId) {
            findViewById(item).setOnClickListener(this);
        }

        mAccountManager = MiAccountManager.get(LoginActivity.this);
        PassportCATokenManager.getInstance().setPassportCAExternal(new PassportCAExternalImpl(getApplicationContext()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshState();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.login_system_btn:
                addSystemAccount();
                break;
            case R.id.login_local_btn:
                addLocalAccount();
                break;
            case R.id.confirm_btn:
                confirmPassword();
                break;
            case R.id.remove_btn:
                removeAccount();
                break;
            case R.id.authToken:
                getAuthToken();
                break;
            case R.id.serviceToken:
                getServiceToken();
                break;
            case R.id.stsUrl:
                getStsUrl();
                break;
            case R.id.cUserId:
                getCUserId();
                break;
            case R.id.getCoreInfo:
                getXiaomiCoreInfo();
                break;
            case R.id.scan_barcode:
                scanBarcode();
                break;
        }
    }

    private void scanBarcode() {
        //
    }

    private void refreshState() {
        Account account = AccountHelper.getXiaomiAccount(this);
        if (account == null) {
            HashMap<Integer, Integer> initialMap = visibility.get(STAGE.INITIAL);
            for (Integer resId : initialMap.keySet()) {
                findViewById(resId).setVisibility(initialMap.get(resId) == View.VISIBLE ? View.VISIBLE : View.GONE);
            }
        } else if (mAccountManager.isUseSystem()) {
            HashMap<Integer, Integer> systemAccountMap = visibility.get(STAGE.SYSTEM_ACCOUNT);
            for (Integer resId : systemAccountMap.keySet()) {
                findViewById(resId).setVisibility(systemAccountMap.get(resId) == View.VISIBLE ? View.VISIBLE : View.GONE);
            }

            ((Button) findViewById(R.id.remove_btn)).setText(R.string.change_account);
            ((TextView) findViewById(R.id.account_id)).setText(getString(R.string.system_account, account.name));
        } else {
            HashMap<Integer, Integer> localAccountMap = visibility.get(STAGE.LOCAL_ACCOUNT);
            for (Integer resId : localAccountMap.keySet()) {
                findViewById(resId).setVisibility(localAccountMap.get(resId) == View.VISIBLE ? View.VISIBLE : View.GONE);
            }

            ((Button) findViewById(R.id.remove_btn)).setText(R.string.remove);
            ((TextView) findViewById(R.id.account_id)).setText(getString(R.string.local_account, account.name));
        }
    }

    private void addSystemAccount() {
        //make MiAccountManager indicate system account(mAccountManager.isUseSystem() return true)
        mAccountManager.setUseSystem();
        checkSystemDangerousPermission();
    }

    //Manifest.permission.GET_ACCOUNTS runtime permission(android sdk > 6.0)
    //system app will has those permissions by default
    private void checkSystemDangerousPermission() {
        new RuntimePermissionHelper.Builder()
                .runnableIfDenied(new Runnable() {
                    @Override
                    public void run() {
                        showInUi("NO permission for GET_ACCOUNT");
                    }
                })
                .runnableIfGranted(new Runnable() {
                    @Override
                    public void run() {
                        Account account = AccountHelper.getXiaomiAccount(LoginActivity.this);
                        if (account == null) {
                            addAccount();
                        } else {
                            refreshState();
                        }
                    }
                })
                .actor(new RuntimePermissionActor.Builder()
                        .activity(LoginActivity.this)
                        .requestCode(1)
                        .requestedPermission(Manifest.permission.GET_ACCOUNTS)
                        .explainDialogOkText(android.R.string.ok)
                        .explainDialogCancelText(android.R.string.cancel)
                        .explainDialogTitle(R.string.get_account_dialog_title)
                        .explainDialogMessage(R.string.get_account_dialog_message)
                        .build())
                .build()
                .checkAndRun();
    }

    private void addLocalAccount() {
        //make MiAccountManager indicate local account(mAccountManager.isUseLocal() return true)
        mAccountManager.setUseLocal();
        addAccount();
    }

    private void addAccount() {
        final String localTAG = "add account";
        Bundle bundle = new Bundle();
        mAccountManager.addAccount(Constants.ACCOUNT_TYPE,
                PASSPORT_API_SID, null, bundle/*if nothing to be pass, can be null*/, LoginActivity.this, new AccountManagerCallback<Bundle>() {
                    @Override
                    public void run(AccountManagerFuture<Bundle> future) {
                        try {
                            Bundle bundle = future.getResult();
                            if (bundle.getBoolean(MiAccountManager.KEY_BOOLEAN_RESULT)) {
                                refreshState();
                                showInUi("Add account succeed!");
                            } else {
                                showInUi("Add account failed or not finished!");
                            }
                        } catch (OperationCanceledException e) {
                            AccountLog.e(TAG, localTAG, e);
                        } catch (AuthenticatorException e) {
                            AccountLog.e(TAG, localTAG, e);
                        } catch (IOException e) {
                            AccountLog.e(TAG, localTAG, e);
                        }
                    }
                }, null);
    }


    protected void confirmPassword() {
        final Account account = AccountHelper.getXiaomiAccount(this);
        if (account == null) {
            return;
        }
        final String localTAG = "confirm password";
        mAccountManager.confirmCredentials(account, null, LoginActivity.this, new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> future) {
                try {
                    Bundle bundle = future.getResult();
                    if (bundle.getBoolean(MiAccountManager.KEY_BOOLEAN_RESULT)) {
                        showInUi("confirmCredentials succeed!");
                    } else {
                        showInUi("confirmCredentials failed or not finished!");
                    }
                } catch (OperationCanceledException e) {
                    AccountLog.e(TAG, localTAG, e);
                } catch (AuthenticatorException e) {
                    AccountLog.e(TAG, localTAG, e);
                } catch (IOException e) {
                    AccountLog.e(TAG, localTAG, e);
                }
            }
        }, null);
    }

    private void removeAccount() {
        if (mAccountManager.isUseSystem()) {
            //if system account, change account to local account
            mAccountManager.setUseLocal();
            refreshState();
        } else if (mAccountManager.isUseLocal()) {
            final Account account = AccountHelper.getXiaomiAccount(this);
            if (account == null) {
                return;
            }
            mAccountManager.removeAccount(account, new AccountManagerCallback<Boolean>() {
                @Override
                public void run(AccountManagerFuture<Boolean> arg0) {
                    refreshState();
                }
            }, null);
        }
    }

    /**
     * 获取系统账号的authtoken，如果当前app不是系统签名，或不在系统的本地白名单中，则获取不到authToken
     * 这种情况有两种解决方式：
     * 1 找安全中心的同学添加白名单；
     * 2 使用下面getServiceToken方法，此方法维护的是一个线上白名单，具体可联系启都
     */
    private void getAuthToken() {
        final Account account = AccountHelper.getXiaomiAccount(this);
        if (account == null) {
            return;
        }
        final String localTAG = "get auth token";
        mAccountManager.getAuthToken(account, PASSPORT_API_SID, null, this, new AccountManagerCallback<Bundle>() {

            @Override
            public void run(AccountManagerFuture<Bundle> accountManagerFuture) {
                try {
                    Bundle bundle = accountManagerFuture.getResult();
                    String authToken = bundle.getString(MiAccountManager.KEY_AUTHTOKEN);
                    if (!TextUtils.isEmpty(authToken)) {
                        showInUi("authToken of type " + PASSPORT_API_SID + " : " + authToken);
                    } else {
                        showInUi("failed to get authToken of type " + PASSPORT_API_SID);
                    }
                } catch (OperationCanceledException e) {
                    AccountLog.e(TAG, localTAG, e);
                } catch (IOException e) {
                    AccountLog.e(TAG, localTAG, e);
                } catch (AuthenticatorException e) {
                    AccountLog.e(TAG, localTAG, e);
                }
            }
        }, null);
    }

    //若要使用此方法获取系统账号的token，将app的签名改成系统签名,或找启都了解白名单机制
    private void getServiceToken() {
        new AsyncTaskBase<ServiceTokenResult>() {

            @Override
            protected ServiceTokenResult doInBackground(Void... params) {
                return mAccountManager.getServiceToken(LoginActivity.this, PASSPORT_API_SID).get();
            }

            @Override
            protected void onPostExecute(ServiceTokenResult serviceTokenResult) {
                super.onPostExecute(serviceTokenResult);
                final String message = serviceTokenResult.toString(
                        ServiceTokenResult.TO_STRING_MASK_SHOW_SERVICETOKEN |
                                ServiceTokenResult.TO_STRING_MASK_SHOW_SECURITY);
                showInUi("serviceTokenResult: " + message);
            }
        }.execute();
    }

    private void getStsUrl() {
        final Account account = AccountHelper.getXiaomiAccount(this);
        if (account == null) {
            return;
        }
        final EditText editText = new EditText(this);
        editText.setHint(R.string.passport_input_password_hint);
        new AlertDialog.Builder(this)
                .setTitle(account.name)
                .setView(editText)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String pwd = editText.getText().toString();
                        if (TextUtils.isEmpty(pwd)) {
                            editText.setError(getString(R.string.passport_error_empty_pwd));
                        } else {
                            getStsUrl(account, pwd, PASSPORT_API_SID);
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void getStsUrl(Account account, String password, String sid) {
        final String localTAG = "get sts url";
        MiLocalFeaturesManager localFeatures = mAccountManager.getLocalFeatures();
        localFeatures.getStsUrl(account.name, password, sid, null, this, new LocalFeaturesManagerCallback<Bundle>() {
            @Override
            public void run(LocalFeaturesManagerFuture<Bundle> future) {
                try {
                    Bundle resBundle = future.getResult();
                    String stsUrl = resBundle.getString(MiAccountManager.KEY_STS_URL);
                    showInUi(TextUtils.isEmpty(stsUrl) ? "null sts url" : "sts url: " + stsUrl);
                } catch (OperationCanceledException e) {
                    AccountLog.e(TAG, localTAG, e);
                } catch (IOException e) {
                    AccountLog.e(TAG, localTAG, e);
                } catch (AuthenticatorException e) {
                    AccountLog.e(TAG, localTAG, e);
                } catch (InvalidCredentialException e) {
                    AccountLog.e(TAG, localTAG, e);
                    //error password
                    showInUi(getString(R.string.passport_error_illegal_pwd));
                } catch (InvalidUserNameException e) {
                    AccountLog.e(TAG, localTAG, e);
                    showInUi(getString(R.string.passport_error_empty_username));
                } catch (AccessDeniedException e) {
                    AccountLog.e(TAG, localTAG, e);
                    showInUi(getString(R.string.passport_access_denied));
                } catch (InvalidResponseException e) {
                    AccountLog.e(TAG, localTAG, e);
                    showInUi(getString(R.string.passport_error_server));
                } catch (IllegalDeviceException e) {
                    AccountLog.e(TAG, localTAG, e);
                    showInUi(getString(R.string.passport_error_device_id));
                } catch (AuthenticationFailureException e) {
                    AccountLog.e(TAG, localTAG, e);
                }
            }
        }, null);
    }

    private void getCUserId() {
        new AsyncTaskBase<String>() {
            @Override
            protected String doInBackground(Void... params) {
                return new CUserIdUtil(LoginActivity.this).getCUserId();
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                showInUi(s);
            }
        }.execute();
    }

    private void getXiaomiCoreInfo() {
        new AsyncTask<Void, Void, XiaomiUserCoreInfo>() {
            @Override
            protected XiaomiUserCoreInfo doInBackground(Void... voids) {
                List<XiaomiUserCoreInfo.Flag> flags = new ArrayList<>();
                flags.add(XiaomiUserCoreInfo.Flag.BASE_INFO);
                try {
                    return AccountHelper.getXiaomiUserCoreInfo(LoginActivity.this, "vr", flags);
                } catch (AccessDeniedException e) {
                    e.printStackTrace();
                } catch (AuthenticationFailureException e) {
                    e.printStackTrace();
                } catch (InvalidResponseException e) {
                    e.printStackTrace();
                } catch (CipherException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(XiaomiUserCoreInfo xiaomiUserCoreInfo) {
                if (xiaomiUserCoreInfo != null) {
                    showInUi("avatar address: " + xiaomiUserCoreInfo.avatarAddress);
                    showInUi("userName: " + xiaomiUserCoreInfo.userName);
                }
            }
        }.execute();

    }

    private void showInUi(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private abstract class AsyncTaskBase<T> extends AsyncTask<Void, Void, T> {
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(LoginActivity.this);
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(T t) {
            progressDialog.dismiss();
        }
    }

}
