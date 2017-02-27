package com.example.huxiandong.network.api.logger;

/**
 * Created by huxiandong
 * on 17-2-27.
 */

public interface ApiLogger {

    void v(String message);

    void v(String format, Object... args);

    void d(String message);

    void d(String format, Object... args);

    void i(String message);

    void i(String format, Object... args);

    void w(String message);

    void w(String format, Object... args);

    void e(String message);

    void e(String format, Object... args);

}
