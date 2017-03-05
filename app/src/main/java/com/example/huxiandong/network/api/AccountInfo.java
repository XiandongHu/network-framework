package com.example.huxiandong.network.api;

import android.accounts.Account;
import android.text.TextUtils;

import com.xiaomi.accountsdk.account.data.ExtendedAuthToken;
import com.xiaomi.passport.accountmanager.MiAccountManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by huxiandong
 * on 17/3/5.
 */

public class AccountInfo {

    public static class PassportInfo {
        private String userId;
        private String cUserId;
        private String passToken;
        private String psecurity;

        private PassportInfo() {
            reset();
        }

        private PassportInfo(String userId, String cUserId, String passToken, String psecurity) {
            this.userId = userId;
            this.cUserId = cUserId;
            this.passToken = passToken;
            this.psecurity = psecurity;
        }

        private boolean isValid() {
            return !TextUtils.isEmpty(userId) && !TextUtils.isEmpty(cUserId)
                    && !TextUtils.isEmpty(passToken) && !TextUtils.isEmpty(psecurity);
        }

        private void reset() {
            this.userId = "";
            this.cUserId = "";
            this.passToken = "";
            this.psecurity = "";
        }

        public String getUserId() {
            return userId;
        }

        public String getCUserId() {
            return cUserId;
        }

        public String getPassToken() {
            return passToken;
        }
    }

    public static class ServiceInfo {
        private String sid;
        private String serviceToken;
        private String ssecurity;

        private ServiceInfo(String sid, String serviceToken, String ssecurity) {
            this.sid = sid;
            this.serviceToken = serviceToken;
            this.ssecurity = ssecurity;
        }

        public String getServiceToken() {
            return serviceToken;
        }
    }

    private static String MICO_SID = ApiConstants.MICO_SID;
    private static String[] EXTRA_SIDS = {
    };

    static AccountInfo load(MiAccountManager accountManager) {
        PassportInfo passportInfo = null;
        Map<String, ServiceInfo> serviceInfoMap = new HashMap<>(EXTRA_SIDS.length + 1);
        Account account = accountManager.getXiaomiAccount();
        if (account != null) {
            String cUserId = accountManager.getUserData(account, "encrypted_user_id");
            ExtendedAuthToken extPass = ExtendedAuthToken.parse(accountManager.getPassword(account));
            ExtendedAuthToken extService = ExtendedAuthToken.parse(accountManager.peekAuthToken(account, MICO_SID));
            if (extPass != null && extService != null) {
                passportInfo = new PassportInfo(account.name, cUserId, extPass.authToken, extPass.security);
                serviceInfoMap.put(MICO_SID, new ServiceInfo(MICO_SID, extService.authToken, extService.security));
                for (String extra_sid : EXTRA_SIDS) {
                    extService = ExtendedAuthToken.parse(accountManager.peekAuthToken(account, extra_sid));
                    if (extService != null) {
                        serviceInfoMap.put(extra_sid, new ServiceInfo(extra_sid, extService.authToken, extService.security));
                    }
                }
            }
        }
        if (passportInfo == null) {
            passportInfo = new PassportInfo();
        }

        return new AccountInfo(passportInfo, serviceInfoMap);
    }

    private PassportInfo mPassportInfo;
    private ConcurrentHashMap<String, ServiceInfo> mServiceInfoMap = new ConcurrentHashMap<>();

    private final Object mPassportLock = new Object();
    private final Object mServiceLock = new Object();

    private AccountInfo(PassportInfo passportInfo, Map<String, ServiceInfo> serviceInfoMap) {
        mPassportInfo = passportInfo;
        mServiceInfoMap.putAll(serviceInfoMap);
    }

    synchronized boolean isValid() {
        return mPassportInfo.isValid() && mServiceInfoMap.containsKey(MICO_SID);
    }

    PassportInfo getPassportInfo() {
        synchronized (mPassportLock) {
            return mPassportInfo;
        }
    }

    ServiceInfo getServiceInfo(String sid) {
        synchronized (mServiceLock) {
            return mServiceInfoMap.get(sid);
        }
    }

    void updatePassportInfo(String userId, String cUserId, String authToken) {
        mPassportInfo.userId = userId;
        mPassportInfo.cUserId = cUserId;
        ExtendedAuthToken extPass = ExtendedAuthToken.parse(authToken);
        if (extPass != null) {
            synchronized (mPassportLock) {
                mPassportInfo.passToken = extPass.authToken;
                mPassportInfo.psecurity = extPass.security;
            }
        }
    }

    void updateServiceInfo(String sid, String authToken) {
        ExtendedAuthToken exService = ExtendedAuthToken.parse(authToken);
        if (exService != null) {
            synchronized (mServiceLock) {
                mServiceInfoMap.put(sid, new ServiceInfo(sid, exService.authToken, exService.security));
            }
        }
    }

    void updateServiceInfo(String sid, String serviceToken, String ssecurity) {
        synchronized (mServiceLock) {
            mServiceInfoMap.put(sid, new ServiceInfo(sid, serviceToken, ssecurity));
        }
    }

    void remove() {
        synchronized (mPassportLock) {
            mPassportInfo.reset();
        }
        synchronized (mServiceLock) {
            mServiceInfoMap.clear();
        }
    }

}
