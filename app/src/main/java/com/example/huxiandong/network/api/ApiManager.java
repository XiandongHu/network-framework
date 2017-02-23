package com.example.huxiandong.network.api;

import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;

import com.example.huxiandong.network.api.adapter.rxjava.RxJavaEnqueueCallAdapterFactory;
import com.example.huxiandong.network.api.model.TopMovie;
import com.example.huxiandong.network.api.service.DoubanService;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.internal.Util;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;

/**
 * Created by huxiandong
 * on 17-2-21.
 */

public class ApiManager {

    private static class SingletonHolder {
        private static final ApiManager INSTANCE = new ApiManager();
    }

    public static ApiManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private Looper mDispatcherLooper;
    private DoubanService mDoubanService;

    private ApiManager() {
        DispatcherThread dispatcherThread = new DispatcherThread();
        dispatcherThread.start();
        mDispatcherLooper = dispatcherThread.getLooper();

        Dispatcher dispatcher = new Dispatcher(new ThreadPoolExecutor(6, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(), Util.threadFactory("OkHttp Dispatcher", false)));
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS);
        Retrofit mRetrofit = new Retrofit.Builder()
                .baseUrl("https://api.douban.com/v2/")
                .client(builder.build())
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaEnqueueCallAdapterFactory.create())
                .build();
        mDoubanService = mRetrofit.create(DoubanService.class);
    }

    public void topMovie(int start, int count, ApiRequest<TopMovie> apiRequest) {
        Observable<Response<TopMovie>> observable = mDoubanService.topMovie(start, count);
        apiRequest.setObservable(observable);
        apiRequest.subscribe(mDispatcherLooper);
    }

    private static class DispatcherThread extends HandlerThread {
        DispatcherThread() {
            super("dispatcher", Process.THREAD_PRIORITY_BACKGROUND);
        }
    }

}
