package com.example.huxiandong.network;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.huxiandong.network.api.LoginManager;
import com.example.huxiandong.network.api.LoginState;
import com.example.huxiandong.network.api.exception.NeedUserInteractionException;
import com.xiaomi.passport.widget.ProgressDialog;

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

    private AccountPermissionRequest mPermissionRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        refreshStatus();
        if (LoginManager.getInstance().hasSystemAccount()
                && TextUtils.isEmpty(LoginManager.getInstance().getSystemAccountUserId())) {
            mPermissionRequest = new AccountPermissionRequest(this, new Runnable() {
                @Override
                public void run() {
                    refreshStatus();
                }
            });
            mPermissionRequest.checkAndRun();
        }
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (mPermissionRequest != null) {
            mPermissionRequest.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
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
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.login_loading));
        dialog.setCancelable(false);
        dialog.show();

        LoginManager.getInstance().loginBySystemAccount(this)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<LoginState>() {
                    @Override
                    public void call(LoginState loginState) {
                        dialog.dismiss();
                        processLoginState(loginState);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        dialog.dismiss();
                        processLoginException(throwable);
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
            Toast.makeText(LoginActivity.this, R.string.login_success, Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(LoginActivity.this, R.string.login_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void processLoginException(Throwable throwable) {
        if (throwable instanceof NeedUserInteractionException) {
            Intent intent = ((NeedUserInteractionException) throwable).getIntent();
            startActivity(intent);
        } else {
            Toast.makeText(LoginActivity.this, R.string.login_failed, Toast.LENGTH_SHORT).show();
        }
    }

}
