package com.example.huxiandong.network;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.TextView;

import com.example.huxiandong.network.api.ApiManager;
import com.example.huxiandong.network.api.model.Contributor;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Subscriber;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.button_click)
    Button mButton;
    @BindView(R.id.text_result)
    TextView mText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.button_click)
    public void onClick() {
        ApiManager.getInstance().repoContributors("square", "retrofit", new Subscriber<List<Contributor>>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(List<Contributor> contributors) {
                StringBuilder sb = new StringBuilder();
                for (Contributor contributor : contributors) {
                    sb.append(contributor.login).append("\n");
                }
                mText.setText(sb);
            }
        });
    }

}
