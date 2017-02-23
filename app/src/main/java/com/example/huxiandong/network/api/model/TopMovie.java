package com.example.huxiandong.network.api.model;

import java.util.List;

/**
 * Created by huxiandong
 * on 17-2-21.
 */

public class TopMovie extends BaseResponse {

    public String title;
    public int start;
    public int count;
    public int total;
    public List<Subject> subjects;

    public static class Subject {
        public String id;
        public String title;
    }

}
