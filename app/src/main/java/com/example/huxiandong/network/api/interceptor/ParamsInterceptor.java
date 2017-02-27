package com.example.huxiandong.network.api.interceptor;

import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by huxiandong
 * on 17-2-24.
 */

public class ParamsInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Request.Builder requestBuilder = request.newBuilder()
                .header("User-Agent", defaultUserAgent());
        if (request.method().equals("GET")) {
            HttpUrl.Builder urlBuilder = request.url().newBuilder();
            urlBuilder.addQueryParameter("deviceId", "xxx");
            requestBuilder.url(urlBuilder.build());
        } else if (request.method().equals("POST") && request.body() instanceof FormBody) {
            // TODO: add form params
        }

        return chain.proceed(requestBuilder.build());
    }

    private String defaultUserAgent() {
        String agent = System.getProperty("http.agent");
        return agent != null ? agent : "Dalvik/2.1.0 (Linux; U; Android 5.0; SM-G9006V Build/LRX21T)";
    }

}
