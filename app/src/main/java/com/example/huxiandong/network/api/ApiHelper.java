package com.example.huxiandong.network.api;

import com.example.huxiandong.network.api.model.TopMovie;

/**
 * Created by huxiandong
 * on 17-2-23.
 */

public class ApiHelper {

    public static ApiRequest topMovie(int start, int count, ApiRequest.Listener<TopMovie> listener) {
        ApiRequest<TopMovie> apiRequest = new ApiRequest<>(listener);
        ApiManager.getInstance().topMovie(start, count, apiRequest);
        return apiRequest;
    }

}
