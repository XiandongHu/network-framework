package com.example.huxiandong.network;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.huxiandong.network.api.ApiError;
import com.example.huxiandong.network.api.ApiHelper;
import com.example.huxiandong.network.api.ApiRequest;
import com.example.huxiandong.network.api.LoginManager;
import com.example.huxiandong.network.api.LoginState;
import com.example.huxiandong.network.api.model.TopMovie;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.login_test)
    Button mLoginTest;
    @BindView(R.id.api_result)
    TextView mApiResult;

    private ApiRequest mApiRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        if (LoginManager.getInstance().hasValidAccount()) {
            mLoginTest.setText(R.string.logout);
        } else {
            mLoginTest.setText(R.string.login_start);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mApiRequest != null) {
            mApiRequest.cancel();
            mApiRequest = null;
        }
    }

    @OnClick({R.id.douban_api_test, R.id.login_test})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.douban_api_test:
                mApiRequest = ApiHelper.topMovie(0, 20, new ApiRequest.Listener<TopMovie>() {
                    @Override
                    public void onSuccess(TopMovie response) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(response.title);
                        for (TopMovie.Subject subject : response.subjects) {
                            sb.append("\n");
                            sb.append("     ");
                            sb.append(subject.title);
                        }
                        mApiResult.setText(sb);
                    }

                    @Override
                    public void onFailure(ApiError apiError) {
                    }
                });
                break;
            case R.id.login_test:
                if (!LoginManager.getInstance().hasValidAccount()) {
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivityForResult(intent, 1000);
                } else {
                    LoginManager.getInstance().logout()
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Action1<LoginState>() {
                                @Override
                                public void call(LoginState loginState) {
                                    if (loginState == LoginState.SUCCESS
                                            || loginState == LoginState.NO_ACCOUNT) {
                                        mLoginTest.setText(R.string.login_start);
                                    }
                                }
                            });
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1000) {
            if (resultCode == RESULT_OK) {
                mLoginTest.setText(R.string.logout);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}
