package com.example.huxiandong.network.api.logger;

import android.util.Log;

/**
 * Created by huxiandong
 * on 17-2-27.
 */

public class AndroidLogger implements ApiLogger {

    private String mTag;

    public AndroidLogger(String tag) {
        mTag = tag;
    }

    @Override
    public void v(String message) {
        Log.v(mTag, message);
    }

    @Override
    public void v(String format, Object... args) {
        Log.v(mTag, String.format(format, args));
    }

    @Override
    public void d(String message) {
        Log.d(mTag, message);
    }

    @Override
    public void d(String format, Object... args) {
        Log.d(mTag, String.format(format, args));
    }

    @Override
    public void i(String message) {
        Log.i(mTag, message);
    }

    @Override
    public void i(String format, Object... args) {
        Log.i(mTag, String.format(format, args));
    }

    @Override
    public void w(String message) {
        Log.w(mTag, message);
    }

    @Override
    public void w(String format, Object... args) {
        Log.w(mTag, String.format(format, args));
    }

    @Override
    public void e(String message) {
        Log.e(mTag, message);
    }

    @Override
    public void e(String format, Object... args) {
        Log.e(mTag, String.format(format, args));
    }

}
