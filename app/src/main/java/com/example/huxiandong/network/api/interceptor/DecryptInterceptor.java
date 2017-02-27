package com.example.huxiandong.network.api.interceptor;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * Created by huxiandong
 * on 17-2-27.
 */

public class DecryptInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        // TODO: decrypt response body if needed
        return chain.proceed(chain.request());
    }

}
