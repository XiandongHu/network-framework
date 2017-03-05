package com.example.huxiandong.network;

import android.app.Application;

import com.example.huxiandong.network.api.ApiManager;

/**
 * Created by huxiandong
 * on 17/3/4.
 */

public class NetworkApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ApiManager.init(this, null);
    }

}
