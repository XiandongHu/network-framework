package com.example.huxiandong.network.api.service;

import com.example.huxiandong.network.api.model.TopMovie;

import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;

/**
 * Created by huxiandong
 * on 17-2-21.
 */

public interface DoubanService {

    @GET("movie/top250")
    Observable<Response<TopMovie>> topMovie(
            @Query("start") int start,
            @Query("count") int count);

}
