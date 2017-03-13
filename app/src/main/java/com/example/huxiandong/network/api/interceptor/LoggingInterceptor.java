package com.example.huxiandong.network.api.interceptor;

import com.example.huxiandong.network.api.logger.ApiLogger;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import okhttp3.Connection;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpHeaders;
import okio.Buffer;
import okio.BufferedSource;

/**
 * Created by huxiandong
 * on 17-2-27.
 */

public class LoggingInterceptor implements Interceptor {

    public enum Level {
        NONE,
        BASIC,
        HEADERS,
        BODY
    }

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private final ApiLogger mLogger;
    private final Level mLevel;

    public LoggingInterceptor(ApiLogger logger, Level level) {
        mLogger = logger;
        mLevel = level;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        if (mLevel == Level.NONE) {
            return chain.proceed(request);
        }

        boolean logBasic = mLevel == Level.BASIC;
        boolean logBody = mLevel == Level.BODY;
        boolean logHeaders = logBody || mLevel == Level.HEADERS;

        RequestBody requestBody = request.body();
        boolean hasRequestBody = requestBody != null;
        Connection connection = chain.connection();
        String protocol = connection != null ? connection.toString() : Protocol.HTTP_1_1.toString();
        String requestStartMessage = "--> " + request.method() + ' ' + request.url() + ' ' + protocol;
        if (logBasic && hasRequestBody) {
            requestStartMessage += " (" + requestBody.contentLength() + "-byte body)";
        }
        mLogger.i(requestStartMessage);

        if (logHeaders) {
            if (hasRequestBody) {
                /*
                  Request body headers are only present when installed as a network interceptor.
                  Force them to be included (when available) so there values are known.
                 */
                if (requestBody.contentType() != null) {
                    mLogger.i("Content-Type: " + requestBody.contentType());
                }
                if (requestBody.contentLength() != -1) {
                    mLogger.i("Content-Length: " + requestBody.contentLength());
                }
            }

            Headers headers = request.headers();
            for (int i = 0, count = headers.size(); i < count; i++) {
                String name = headers.name(i);
                // Skip headers from the request body as they are explicitly logged above.
                if (!"Content-Type".equalsIgnoreCase(name) && !"Content-Length".equalsIgnoreCase(name)) {
                    mLogger.i(name + ": " + headers.value(i));
                }
            }

            if (!logBody || !hasRequestBody) {
                mLogger.i("--> END " + request.method());
            } else if (bodyEncoded(request.headers())) {
                mLogger.i("--> END " + request.method() + " (encoded body omitted)");
            } else {
                Buffer buffer = new Buffer();
                requestBody.writeTo(buffer);

                Charset charset = UTF8;
                MediaType contentType = requestBody.contentType();
                if (contentType != null) {
                    charset = contentType.charset(UTF8);
                }

                if (isPlaintext(buffer)) {
                    mLogger.i(buffer.readString(charset));
                    mLogger.i("--> END " + request.method()
                            + " (" + requestBody.contentLength() + "-byte body)");
                } else {
                    mLogger.i("--> END " + request.method() + " (binary "
                            + requestBody.contentLength() + "-byte body omitted)");
                }
            }
        }

        long startNs = System.nanoTime();
        Response response;
        try {
            response = chain.proceed(request);
        } catch (Exception e) {
            mLogger.i("<-- HTTP FAILED: " + e);
            throw e;
        }
        long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

        ResponseBody responseBody = response.body();
        long contentLength = responseBody.contentLength();
        String bodySize = contentLength != -1 ? contentLength + "-byte" : "unknown-length";
        mLogger.i("<-- " + response.code() + ' ' + response.message() + ' '
                + response.request().url() + " (" + tookMs + "ms" + (logBasic ? ", "
                + bodySize + " body" : "") + ')');

        if (logHeaders) {
            Headers headers = response.headers();
            for (int i = 0, count = headers.size(); i < count; i++) {
                mLogger.i(headers.name(i) + ": " + headers.value(i));
            }

            /*
              If Content-Encoding is gzip in response, we can not log response body when
              installed as a network interceptor. Because unzip gzip-body in BridgeInterceptor.
             */
            if (!logBody || !HttpHeaders.hasBody(response)) {
                mLogger.i("<-- END HTTP");
            } else if (bodyEncoded(response.headers())) {
                mLogger.i("<-- END HTTP (encoded body omitted)");
            } else {
                BufferedSource source = responseBody.source();
                source.request(5000); // Source request in units of 8K
                Buffer buffer = source.buffer();

                Charset charset = UTF8;
                MediaType contentType = responseBody.contentType();
                if (contentType != null) {
                    charset = contentType.charset(UTF8);
                }

                if (!isPlaintext(buffer)) {
                    mLogger.i("<-- END HTTP (binary " + buffer.size() + "-byte body omitted)");
                    return response;
                }

                /*
                  Unzip gzip-body will remove Content-Length in BridgeInterceptor. Default
                  Content-Length is -1, so also can log.
                 */
                if (contentLength != 0) {
                    mLogger.i(buffer.clone().readString(charset));
                }
                mLogger.i("<-- END HTTP (" + buffer.size() + "-byte body)");
            }
        }

        return response;
    }

    /**
     * Returns true if the body in question probably contains human readable text. Uses a small sample
     * of code points to detect unicode control characters commonly used in binary file signatures.
     */
    private static boolean isPlaintext(Buffer buffer) {
        try {
            Buffer prefix = new Buffer();
            long byteCount = buffer.size() < 64 ? buffer.size() : 64;
            buffer.copyTo(prefix, 0, byteCount);
            for (int i = 0; i < 16; i++) {
                if (prefix.exhausted()) {
                    break;
                }
                int codePoint = prefix.readUtf8CodePoint();
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false;
                }
            }
            return true;
        } catch (EOFException e) {
            return false; // Truncated UTF-8 sequence.
        }
    }

    private static boolean bodyEncoded(Headers headers) {
        String contentEncoding = headers.get("Content-Encoding");
        return contentEncoding != null && !contentEncoding.equalsIgnoreCase("identity");
    }

}