package com.example.huxiandong.network.api;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by huxiandong
 * on 17/3/5.
 */

class ApiConstants {

    static final String MICO_BASE_URL = "https://staging.api.mina.mi.com/";
    static final String MICO_SID = "mico_staging";

    static final Map<String, String> sidToUrl = new HashMap<String, String>() {
        private static final long serialVersionUID = -6215372541386342673L;

        {
            put(MICO_SID, MICO_BASE_URL);
        }
    };

}
