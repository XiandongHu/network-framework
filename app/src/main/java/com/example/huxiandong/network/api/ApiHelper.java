package com.example.huxiandong.network.api;

import com.example.huxiandong.network.api.model.TopMovie;
import com.example.huxiandong.network.api.service.DoubanService;

import retrofit2.Response;
import rx.Observable;

/**
 * Created by huxiandong
 * on 17-2-23.
 */

public class ApiHelper {

    public static ApiRequest topMovie(final int start, final int count, ApiRequest.Listener<TopMovie> listener) {
        return ApiManager.getInstance().enqueue(new ApiProvider<TopMovie>() {
            @Override
            public Observable<Response<TopMovie>> observable(DoubanService service) {
                return service.topMovie(start, count);
            }
        }, listener);
    }

}
