package com.example.huxiandong.network.api.interceptor;

import java.io.IOException;
import java.util.Random;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by huxiandong
 * on 17-2-24.
 */

public class ParamsInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Request.Builder requestBuilder = request.newBuilder();
        if (request.method().equals("GET")) {
            HttpUrl httpUrl = request.url().newBuilder()
                    .addQueryParameter("requestId", randomRequestId())
                    .build();
            requestBuilder.url(httpUrl);
        } else if (request.method().equals("POST")) {
            RequestBody requestBody = request.body();
            if (requestBody instanceof FormBody) {
                FormBody.Builder formBodyBuilder = new FormBody.Builder();
                FormBody formBody = (FormBody) requestBody;
                for (int i = 0; i < formBody.size(); i++) {
                    formBodyBuilder.addEncoded(formBody.encodedName(i), formBody.encodedValue(i));
                }
                formBody = formBodyBuilder.addEncoded("requestId", randomRequestId())
                        .build();
                requestBuilder.post(formBody);
            }
        }
        return chain.proceed(requestBuilder.build());
    }

    private static String randomRequestId() {
        String optionChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            int index = random.nextInt(optionChars.length());
            sb.append(optionChars.charAt(index));
        }
        return sb.toString();
    }

}
