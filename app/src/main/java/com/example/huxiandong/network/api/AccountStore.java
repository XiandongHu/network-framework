package com.example.huxiandong.network.api;

/**
 * Created by huxiandong
 * on 17/3/6.
 */

interface AccountStore {

    AccountType getAccountType();

    void setAccountType(AccountType accountType);

    AccountInfo loadAccountInfo();

    void saveAccountInfo(AccountInfo accountInfo);

    void removeAccountInfo();

}
