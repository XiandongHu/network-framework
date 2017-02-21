package com.example.huxiandong.network.api;

import com.example.huxiandong.network.api.model.Contributor;
import com.example.huxiandong.network.api.service.GitHubService;

import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by huxiandong
 * on 17-2-21.
 */

public class ApiManager {

    private Retrofit mRetrofit;
    private GitHubService mGitHubService;

    private static class SingletonHolder {
        private static final ApiManager INSTANCE = new ApiManager();
    }

    public static ApiManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private ApiManager() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS);
        mRetrofit = new Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .client(builder.build())
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io()))
                .build();
        mGitHubService = mRetrofit.create(GitHubService.class);
    }

    public Subscription repoContributors(String owner, String repo, Subscriber<List<Contributor>> subscriber) {
        return mGitHubService.repoContributors(owner, repo)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(subscriber);
    }

}
