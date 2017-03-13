package com.example.huxiandong.network.api;

import rx.Scheduler;

/**
 * Created by huxiandong
 * on 17-3-13.
 */

public interface ApiResponseHandler {

    Scheduler getScheduler();

    void retry(ApiRequest apiRequest);

    void cancel(ApiRequest apiRequest);

}
