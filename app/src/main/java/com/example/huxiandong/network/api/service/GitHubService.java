package com.example.huxiandong.network.api.service;

import com.example.huxiandong.network.api.model.Contributor;

import java.util.List;

import retrofit2.http.GET;
import retrofit2.http.Path;
import rx.Observable;

/**
 * Created by huxiandong
 * on 17-2-21.
 */

public interface GitHubService {

    @GET("/repos/{owner}/{repo}/contributors")
    Observable<List<Contributor>> repoContributors(
            @Path("owner") String owner,
            @Path("repo") String repo);

}
