package com.example.huxiandong.network.api.interceptor;

import com.example.huxiandong.network.api.logger.ApiLogger;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by huxiandong
 * on 17-2-27.
 */

public class LoggingInterceptor implements Interceptor {

    private ApiLogger mLogger;

    public LoggingInterceptor(ApiLogger logger) {
        mLogger = logger;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        long t1 = System.nanoTime();
        mLogger.i("Sending request %s on %s\n%s", request.url(), chain.connection(), request.headers());

        Response response = chain.proceed(request);
        long t2 = System.nanoTime();
        mLogger.i("Received response for %s in %.1fms\n%s", request.url(), (t2 - t1) / 1e6d, response.headers());

        return response;
    }

}
