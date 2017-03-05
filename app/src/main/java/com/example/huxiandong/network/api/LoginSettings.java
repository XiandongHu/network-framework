package com.example.huxiandong.network.api;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by huxiandong
 * on 17/3/5.
 */

class LoginSettings {

    private static final String SP_NAME = "LoginSettings";
    private static final String KEY_MODE = "mode";

    enum Mode {
        UNKNOWN,
        SYSTEM,
        LOCAL,
        NONE
    }

    private Context mContext;
    private final Object mModeLock = new Object();

    LoginSettings(Context context) {
        mContext = context;
    }

    Mode getMode() {
        synchronized (mModeLock) {
            int mode = getSP().getInt(KEY_MODE, -1);
            Mode[] modes = Mode.values();
            return (mode >= 0 && mode < modes.length) ? modes[mode] : Mode.UNKNOWN;
        }
    }

    void setMode(Mode mode) {
        synchronized (mModeLock) {
            getSP().edit().putInt(KEY_MODE, mode.ordinal()).apply();
        }
    }

    private SharedPreferences getSP() {
        return mContext.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
    }

}
