package com.example.huxiandong.network;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.TextView;

import com.example.huxiandong.network.api.ApiHelper;
import com.example.huxiandong.network.api.ApiRequest;
import com.example.huxiandong.network.api.model.TopMovie;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.button_click)
    Button mButton;
    @BindView(R.id.text_result)
    TextView mText;

    private ApiRequest mApiRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mApiRequest != null) {
            mApiRequest.cancel();
            mApiRequest = null;
        }
    }

    @OnClick(R.id.button_click)
    public void onClick() {
        mApiRequest = ApiHelper.topMovie(0, 10, new ApiRequest.Listener<TopMovie>() {
            @Override
            public void onSuccess(TopMovie response) {
                StringBuilder sb = new StringBuilder();
                sb.append(response.title);
                for (TopMovie.Subject subject : response.subjects) {
                    sb.append("\n");
                    sb.append("     ");
                    sb.append(subject.title);
                }
                mText.setText(sb);
            }

            @Override
            public void onFailure(Throwable t) {
            }
        });
    }

}
