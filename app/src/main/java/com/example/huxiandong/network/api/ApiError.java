package com.example.huxiandong.network.api;

/**
 * Created by huxiandong
 * on 17-3-13.
 */

public class ApiError {

    public static final ApiError TOKEN_INSUFFICIENT;
    public static final ApiError TOKEN_INVALID;
    public static final ApiError HTTP_ENGINE_EXCEPTION;
    public static final ApiError HTTP_TIMEOUT;
    public static final ApiError HTTP_CODE_4XX;
    public static final ApiError HTTP_CODE_5XX;
    public static final ApiError HTTP_CODE_OTHER;

    static {
        TOKEN_INSUFFICIENT = generateError(10001, "Token Insufficiency");
        TOKEN_INVALID = generateError(10002, "Token Insufficiency");
        HTTP_ENGINE_EXCEPTION = generateError(10003, "Http Engine Exception");
        HTTP_TIMEOUT = generateError(10004, "Http Timeout");
        HTTP_CODE_4XX = generateError(10005, "Bad Request");
        HTTP_CODE_5XX = generateError(10006, "Internal Server Error");
        HTTP_CODE_OTHER = generateError(10007, "Other Http Error Code");
    }

    static ApiError generateError(int code, String message) {
        return new ApiError(code, message);
    }

    private int mCode;
    private String mMessage;

    private ApiError(int code, String message) {
        mCode = code;
        mMessage = message;
    }

    public int getCode() {
        return mCode;
    }

    public String getMessage() {
        return mMessage;
    }

    @Override
    public boolean equals(Object o) {
        return o == this || (o instanceof ApiError && ((ApiError) o).mCode == mCode);
    }

}
