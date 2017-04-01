package com.example.huxiandong.network.api.exception;

import android.content.Intent;

/**
 * Created by huxiandong
 * on 17-4-1.
 */

public class NeedUserInteractionException extends Exception {

    private static final long serialVersionUID = 4951225316343246487L;

    private Intent mIntent;

    public NeedUserInteractionException(Intent intent) {
        super("User Interaction Needed.");
        mIntent = intent;
    }

    public Intent getIntent() {
        return mIntent;
    }

}
