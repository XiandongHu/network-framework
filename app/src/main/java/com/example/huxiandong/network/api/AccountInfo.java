package com.example.huxiandong.network.api;

import android.accounts.Account;
import android.text.TextUtils;

import com.xiaomi.accountsdk.account.data.ExtendedAuthToken;
import com.xiaomi.passport.accountmanager.MiAccountManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
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

        private boolean isValid() {
            return !TextUtils.isEmpty(userId) && !TextUtils.isEmpty(cUserId);
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

    private static String CORE_SID = ApiConstants.MICO_SID;

    static AccountInfo load(MiAccountManager accountManager, AccountStore accountStore) {
        AccountInfo accountInfo = null;
        if (accountManager.getXiaomiAccount() == null) {
            AccountType accountType = accountStore.getAccountType();
            if (accountType != AccountType.UNKNOWN && accountType != AccountType.NONE) {
                accountStore.setAccountType(AccountType.NONE);
            }
            accountStore.removeAccountInfo();
        } else {
            accountInfo = accountStore.loadAccountInfo();
        }
        if (accountInfo == null) {
            accountInfo = new AccountInfo();
        }

        return accountInfo;
    }

    static AccountInfo newAccountInfo(PassportInfo passportInfo, Map<String, ServiceInfo> serviceInfoMap) {
        return new AccountInfo(passportInfo, serviceInfoMap);
    }

    private PassportInfo mPassportInfo = new PassportInfo();
    private Map<String, ServiceInfo> mServiceInfoMap = new ConcurrentHashMap<>();

    private final Object mPassportLock = new Object();

    private AccountInfo() {
    }

    private AccountInfo(PassportInfo passportInfo, Map<String, ServiceInfo> serviceInfoMap) {
        mPassportInfo = passportInfo;
        if (serviceInfoMap != null && !serviceInfoMap.isEmpty()) {
            mServiceInfoMap.putAll(serviceInfoMap);
        }
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
        String password;
        try {
            password = accountManager.getPassword(account);
        } catch (SecurityException e) {
            password = null;
        }
        updatePassportInfo(account.name, cUserId, password);
        updateServiceInfo(CORE_SID, authToken);

        if (accountManager.isUseSystem()) {
            accountStore.setAccountType(AccountType.SYSTEM);
        } else {
            accountStore.setAccountType(AccountType.LOCAL);
        }
        accountStore.saveAccountInfo(this);
    }

    private void updatePassportInfo(String userId, String cUserId, String authToken) {
        synchronized (mPassportLock) {
            mPassportInfo.userId = userId;
            mPassportInfo.cUserId = cUserId;
            ExtendedAuthToken extendedAuthToken = ExtendedAuthToken.parse(authToken);
            if (extendedAuthToken != null) {
                mPassportInfo.passToken = extendedAuthToken.authToken;
                mPassportInfo.psecurity = extendedAuthToken.security;
            }
        }
    }

    private void updateServiceInfo(String sid, String authToken) {
        ExtendedAuthToken extendedAuthToken = ExtendedAuthToken.parse(authToken);
        if (extendedAuthToken != null) {
            mServiceInfoMap.put(sid, new ServiceInfo(sid, extendedAuthToken.authToken, extendedAuthToken.security));
        }
    }

    void updateServiceInfo(AccountStore accountStore, String sid, String serviceToken, String ssecurity) {
        mServiceInfoMap.put(sid, new ServiceInfo(sid, serviceToken, ssecurity));
        accountStore.saveAccountInfo(this);
    }

    void remove(AccountStore accountStore) {
        synchronized (mPassportLock) {
            mPassportInfo.reset();
        }
        mServiceInfoMap.clear();

        accountStore.setAccountType(AccountType.NONE);
        accountStore.removeAccountInfo();
    }

}
