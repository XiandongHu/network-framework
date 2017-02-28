package com.example.huxiandong.network.api.interceptor;

import com.example.huxiandong.network.api.logger.ApiLogger;

import java.io.IOException;
import java.nio.charset.Charset;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;

/**
 * Created by huxiandong
 * on 17-2-27.
 */

public class LoggingInterceptor implements Interceptor {

    private static final Charset UTF8 = Charset.forName("UTF-8");

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
        ResponseBody body = response.body();
        if (body != null && body.contentLength() > 0) {
            BufferedSource source = body.source();
            source.request(8192);
            Buffer buffer = source.buffer();
            Charset charset = UTF8;
            MediaType contentType = body.contentType();
            if (contentType != null) {
                charset = contentType.charset(UTF8);
            }
            long t2 = System.nanoTime();
            mLogger.i("Received response for %s in %.1fms\n%s", request.url(), (t2 - t1) / 1e6d, response.headers());
            mLogger.i("Body <-- %s", buffer.clone().readString(charset));
        } else {
            long t2 = System.nanoTime();
            mLogger.i("Received response for %s in %.1fms\n%s", request.url(), (t2 - t1) / 1e6d, response.headers());
            mLogger.i("No Body <--");
        }

        return response;
    }

}
