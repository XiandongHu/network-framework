package com.example.huxiandong.network.api;

import android.accounts.Account;
import android.text.TextUtils;

import com.xiaomi.accountsdk.account.data.ExtendedAuthToken;
import com.xiaomi.passport.accountmanager.MiAccountManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by huxiandong
 * on 17/3/5.
 */

public class AccountInfo {

    public static class PassportInfo implements Serializable {
        private static final long serialVersionUID = 4196400872621314050L;

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

        private void writeObject(ObjectOutputStream out) throws IOException {
            out.writeObject(userId);
            out.writeObject(cUserId);
            out.writeObject(passToken);
            out.writeObject(psecurity);
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            userId = (String) in.readObject();
            cUserId = (String) in.readObject();
            passToken = (String) in.readObject();
            psecurity = (String) in.readObject();
        }
    }

    public static class ServiceInfo implements Serializable {
        private static final long serialVersionUID = -1387759219905842987L;

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

        private void writeObject(ObjectOutputStream out) throws IOException {
            out.writeObject(sid);
            out.writeObject(serviceToken);
            out.writeObject(ssecurity);
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            sid = (String) in.readObject();
            serviceToken = (String) in.readObject();
            ssecurity = (String) in.readObject();
        }
    }

    static final String C_USER_ID_KEY = "encrypted_user_id";
    private static String CORE_SID = ApiConstants.MICO_SID;
    private static String[] EXTRA_SIDS = {
    };

    static AccountInfo load(MiAccountManager accountManager, AccountStore accountStore) {
        PassportInfo passportInfo = null;
        Map<String, ServiceInfo> serviceInfoMap = new HashMap<>(EXTRA_SIDS.length + 1);
        if (accountManager.isUseLocal()) {
            Account account = accountManager.getXiaomiAccount();
            if (account != null) {
                String cUserId = accountManager.getUserData(account, C_USER_ID_KEY);
                ExtendedAuthToken extPass = ExtendedAuthToken.parse(accountManager.getPassword(account));
                ExtendedAuthToken extService = ExtendedAuthToken.parse(accountManager.peekAuthToken(account, CORE_SID));
                if (extPass != null && extService != null) {
                    passportInfo = new PassportInfo(account.name, cUserId, extPass.authToken, extPass.security);
                    serviceInfoMap.put(CORE_SID, new ServiceInfo(CORE_SID, extService.authToken, extService.security));
                    for (String extra_sid : EXTRA_SIDS) {
                        extService = ExtendedAuthToken.parse(accountManager.peekAuthToken(account, extra_sid));
                        if (extService != null) {
                            serviceInfoMap.put(extra_sid, new ServiceInfo(extra_sid, extService.authToken, extService.security));
                        }
                    }
                }
            }
        } else {
            if (!accountManager.canUseSystem() && accountStore.getAccountType() == AccountType.SYSTEM) {
                accountStore.setAccountType(AccountType.NONE);
                accountStore.removeAccountInfo();
            } else {
                AccountInfo accountInfo = accountStore.loadAccountInfo();
                if (accountInfo != null) {
                    return accountInfo;
                }
            }
        }
        if (passportInfo == null) {
            passportInfo = new PassportInfo();
        }

        return newAccountInfo(passportInfo, serviceInfoMap);
    }

    static AccountInfo newAccountInfo(PassportInfo passportInfo, Map<String, ServiceInfo> serviceInfoMap) {
        return new AccountInfo(passportInfo, serviceInfoMap);
    }

    private PassportInfo mPassportInfo;
    private Map<String, ServiceInfo> mServiceInfoMap = new ConcurrentHashMap<>();

    private final Object mPassportLock = new Object();

    private AccountInfo(PassportInfo passportInfo, Map<String, ServiceInfo> serviceInfoMap) {
        mPassportInfo = passportInfo;
        mServiceInfoMap.putAll(serviceInfoMap);
    }

    PassportInfo getPassportInfo() {
        synchronized (mPassportLock) {
            return mPassportInfo;
        }
    }

    Map<String, ServiceInfo> getServiceInfoMap() {
        return mServiceInfoMap;
    }

    ServiceInfo getServiceInfo(String sid) {
        return mServiceInfoMap.get(sid);
    }

    synchronized boolean isValid() {
        return mPassportInfo.isValid() && mServiceInfoMap.containsKey(CORE_SID);
    }

    void updateAccountCoreInfo(MiAccountManager accountManager, AccountStore accountStore,
                               Account account, String cUserId, String authToken) {
        String password = accountManager.getPassword(account);
        updatePassportInfo(account.name, cUserId, password);
        updateServiceInfo(CORE_SID, authToken);
        if (accountManager.isUseSystem()) {
            accountStore.saveAccountInfo(this);
        }
    }

    private void updatePassportInfo(String userId, String cUserId, String authToken) {
        ExtendedAuthToken extPass = ExtendedAuthToken.parse(authToken);
        if (extPass != null) {
            synchronized (mPassportLock) {
                mPassportInfo.userId = userId;
                mPassportInfo.cUserId = cUserId;
                mPassportInfo.passToken = extPass.authToken;
                mPassportInfo.psecurity = extPass.security;
            }
        }
    }

    private void updateServiceInfo(String sid, String authToken) {
        ExtendedAuthToken exService = ExtendedAuthToken.parse(authToken);
        if (exService != null) {
            mServiceInfoMap.put(sid, new ServiceInfo(sid, exService.authToken, exService.security));
        }
    }

    void updateServiceInfo(MiAccountManager accountManager, AccountStore accountStore,
                           String sid, String serviceToken, String ssecurity) {
        mServiceInfoMap.put(sid, new ServiceInfo(sid, serviceToken, ssecurity));
        if (accountManager.isUseSystem()) {
            accountStore.saveAccountInfo(this);
        }
    }

    void remove(MiAccountManager accountManager, AccountStore accountStore) {
        synchronized (mPassportLock) {
            mPassportInfo.reset();
        }
        mServiceInfoMap.clear();
        if (accountManager.isUseSystem()) {
            accountStore.removeAccountInfo();
        }
    }

}
