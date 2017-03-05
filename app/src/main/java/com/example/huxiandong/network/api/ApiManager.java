package com.example.huxiandong.network.api;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;

import com.example.huxiandong.network.api.adapter.rxjava.RxJavaEnqueueCallAdapterFactory;
import com.example.huxiandong.network.api.interceptor.DecryptInterceptor;
import com.example.huxiandong.network.api.interceptor.LoggingInterceptor;
import com.example.huxiandong.network.api.interceptor.ParamsInterceptor;
import com.example.huxiandong.network.api.logger.AndroidLogger;
import com.example.huxiandong.network.api.logger.ApiLogger;
import com.example.huxiandong.network.api.model.BaseResponse;
import com.example.huxiandong.network.api.service.DoubanService;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.internal.Util;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by huxiandong
 * on 17-2-21.
 */

public class ApiManager {

    @SuppressLint("StaticFieldLeak")
    private static ApiManager sInstance;

    public static void init(Context context, ApiLogger apiLogger) {
        if (sInstance != null) {
            throw new IllegalStateException("ApiManager has already been initialized.");
        }
        if (apiLogger == null) {
            apiLogger = new AndroidLogger("ApiManager");
        }
        sInstance = new ApiManager(context, apiLogger);
    }

    static ApiManager getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("ApiManager has not been initialized.");
        }
        return sInstance;
    }

    private Context mContext;
    private Scheduler mDispatcherScheduler;
    private DoubanService mDoubanService;

    private ApiLogger mApiLogger;

    private ApiManager(Context context, ApiLogger apiLogger) {
        mContext = context;
        mApiLogger = apiLogger;

        DispatcherThread dispatcherThread = new DispatcherThread();
        dispatcherThread.start();
        Looper looper = dispatcherThread.getLooper();
        mDispatcherScheduler = AndroidSchedulers.from(looper);

        Dispatcher dispatcher = new Dispatcher(new ThreadPoolExecutor(6, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(), Util.threadFactory("OkHttp Dispatcher", false)));
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .addInterceptor(new ParamsInterceptor())
                .addNetworkInterceptor(new LoggingInterceptor(mApiLogger))
                .addNetworkInterceptor(new DecryptInterceptor())
                .cookieJar(LoginManager.init(mContext, mDispatcherScheduler, new Handler(looper), mApiLogger));
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.douban.com/v2/")
                .client(builder.build())
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaEnqueueCallAdapterFactory.create())
                .build();
        mDoubanService = retrofit.create(DoubanService.class);
    }

    <T extends BaseResponse> ApiRequest<T> enqueue(final ApiProvider<T> provider, ApiRequest.Listener<T> listener) {
        final ApiRequest<T> apiRequest = new ApiRequest<>(listener);
        Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
                try {
                    apiRequest.setObservable(provider.observable(mDoubanService));
                    if (!apiRequest.isCanceled()) {
                        apiRequest.subscribe(mDispatcherScheduler);
                    }
                } catch (Throwable e) {
                    subscriber.onError(e);
                    return;
                }
                subscriber.onNext(null);
                subscriber.onCompleted();
            }
        }).subscribeOn(mDispatcherScheduler).subscribe();
        return apiRequest;
    }

    private static class DispatcherThread extends HandlerThread {
        DispatcherThread() {
            super("dispatcher", Process.THREAD_PRIORITY_BACKGROUND);
        }
    }

}
