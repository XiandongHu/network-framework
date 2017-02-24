package com.example.huxiandong.network.api;

import com.example.huxiandong.network.api.model.BaseResponse;
import com.example.huxiandong.network.api.service.DoubanService;

import retrofit2.Response;
import rx.Observable;

/**
 * Created by huxiandong
 * on 17-2-24.
 */

public interface ApiProvider<T extends BaseResponse> {

    Observable<Response<T>> observable(DoubanService service);

}
