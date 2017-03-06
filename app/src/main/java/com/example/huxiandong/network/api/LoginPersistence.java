package com.example.huxiandong.network.api;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by huxiandong
 * on 17/3/5.
 */

class LoginPersistence {

    private static final String SP_NAME = "LoginPersistence";
    private static final String KEY_MODE = "mode";
    private static final String KEY_ENCODED_ACCOUNT = "encoded_account";

    enum Mode {
        UNKNOWN,
        SYSTEM,
        LOCAL,
        NONE
    }

    private Context mContext;
    private final Object mModeLock = new Object();

    LoginPersistence(Context context) {
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

    String getEncodedAccount() {
        return getSP().getString(KEY_ENCODED_ACCOUNT, null);
    }

    void setEncodedAccount(String encodedAccount) {
        getSP().edit().putString(KEY_ENCODED_ACCOUNT, encodedAccount).apply();
    }

    void resetEncodingAccount() {
        getSP().edit().remove(KEY_ENCODED_ACCOUNT).apply();
    }

    private SharedPreferences getSP() {
        return mContext.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
    }

}
