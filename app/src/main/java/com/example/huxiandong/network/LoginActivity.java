package com.example.huxiandong.network;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.huxiandong.network.api.LoginManager;
import com.example.huxiandong.network.api.LoginState;
import com.xiaomi.accounts.Manifest;
import com.xiaomi.passport.utils.RuntimePermissionActor;
import com.xiaomi.passport.utils.RuntimePermissionHelper;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by huxiandong
 * on 17-3-6.
 */

public class LoginActivity extends AppCompatActivity {

    @BindView(R.id.text_status)
    TextView mStatus;
    @BindView(R.id.button_system_login)
    Button mSystemLogin;
    @BindView(R.id.button_local_login)
    Button mLocalLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        refreshStatus();
        if (LoginManager.getInstance().hasSystemAccount()
                && TextUtils.isEmpty(LoginManager.getInstance().getSystemAccountUserId()))
            checkSystemDangerousPermission();
    }

    @OnClick({R.id.button_system_login, R.id.button_local_login})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_system_login:
                loginBySystemAccount();
                break;
            case R.id.button_local_login:
                loginByLocalAccount();
                break;
        }
    }

    private void checkSystemDangerousPermission() {
        new RuntimePermissionHelper.Builder()
                .runnableIfGranted(new Runnable() {
                    @Override
                    public void run() {
                        refreshStatus();
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

    private void refreshStatus() {
        String userId = LoginManager.getInstance().getSystemAccountUserId();
        if (!TextUtils.isEmpty(userId)) {
            mStatus.setText(getString(R.string.login_account, userId));
            mSystemLogin.setVisibility(View.VISIBLE);
        } else {
            mStatus.setText(R.string.login_account_not_found);
            mSystemLogin.setVisibility(View.GONE);
        }
    }

    private void loginBySystemAccount() {
        LoginManager.getInstance().loginBySystemAccount(this)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<LoginState>() {
                    @Override
                    public void call(LoginState loginState) {
                        processLoginState(loginState);
                    }
                });
    }

    private void loginByLocalAccount() {
        LoginManager.getInstance().loginByLocalAccount(this)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<LoginState>() {
                    @Override
                    public void call(LoginState loginState) {
                        processLoginState(loginState);
                    }
                });
    }

    private void processLoginState(LoginState loginState) {
        if (loginState == LoginState.SUCCESS) {
            Toast.makeText(LoginActivity.this, R.string.toast_login_success, Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(LoginActivity.this, R.string.toast_login_failed, Toast.LENGTH_SHORT).show();
        }
    }

}
