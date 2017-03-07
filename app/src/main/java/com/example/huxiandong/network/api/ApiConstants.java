package com.example.huxiandong.network.api;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by huxiandong
 * on 17/3/5.
 */

class ApiConstants {

    static final String MICO_BASE_URL = "https://api.mico.com/";
    static final String MICO_SID = "mico_staging";

    static final Map<String, String> sidToUrl = new HashMap<String, String>() {
        {
            put(MICO_SID, MICO_BASE_URL);
        }
    };

}
