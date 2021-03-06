package com.example.huxiandong.network.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;

/**
 * Created by huxiandong
 * on 17/3/5.
 */

class SharedPrefsStore implements AccountStore {

    private static final String SP_NAME = "Account";
    private static final String KEY_ACCOUNT_TYPE = "account_type";
    private static final String KEY_ENCODED_ACCOUNT = "encoded_account";

    private Context mContext;

    SharedPrefsStore(Context context) {
        mContext = context;
    }

    private SharedPreferences getSP() {
        return mContext.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public AccountType getAccountType() {
        int accountType = getSP().getInt(KEY_ACCOUNT_TYPE, -1);
        AccountType[] accountTypes = AccountType.values();
        return (accountType >= 0 && accountType < accountTypes.length) ? accountTypes[accountType] : AccountType.UNKNOWN;
    }

    @Override
    public void setAccountType(AccountType accountType) {
        getSP().edit().putInt(KEY_ACCOUNT_TYPE, accountType.ordinal()).apply();
    }

    @Override
    public AccountInfo loadAccountInfo() {
        String encodedAccountInfo = getSP().getString(KEY_ENCODED_ACCOUNT, null);
        if (!TextUtils.isEmpty(encodedAccountInfo)) {
            return new SerializableAccountInfo().decode(encodedAccountInfo);
        }
        return null;
    }

    @Override
    public void saveAccountInfo(AccountInfo accountInfo) {
        String encodedAccountInfo = new SerializableAccountInfo().encode(accountInfo);
        getSP().edit().putString(KEY_ENCODED_ACCOUNT, encodedAccountInfo).apply();
    }

    @Override
    public void removeAccountInfo() {
        getSP().edit().remove(KEY_ENCODED_ACCOUNT).apply();
    }

    private static class SerializableAccountInfo implements Serializable {
        private static final long serialVersionUID = -5499321915333515087L;

        private transient AccountInfo accountInfo;

        String encode(AccountInfo accountInfo) {
            this.accountInfo = accountInfo;

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = null;
            try {
                objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
                objectOutputStream.writeObject(this);
            } catch (IOException e) {
                return null;
            } finally {
                if (objectOutputStream != null) {
                    try {
                        // Closing a ByteArrayOutputStream has no effect, it can be used later (and is used in the return statement)
                        objectOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            return byteArrayToHexString(byteArrayOutputStream.toByteArray());
        }

        private static String byteArrayToHexString(byte[] bytes) {
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte element : bytes) {
                int v = element & 0xff;
                if (v < 16) {
                    sb.append('0');
                }
                sb.append(Integer.toHexString(v));
            }
            return sb.toString();
        }

        AccountInfo decode(String encodedAccountInfo) {
            AccountInfo accountInfo = null;

            byte[] bytes = hexStringToByteArray(encodedAccountInfo);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            ObjectInputStream objectInputStream = null;
            try {
                objectInputStream = new ObjectInputStream(byteArrayInputStream);
                accountInfo = ((SerializableAccountInfo) objectInputStream.readObject()).accountInfo;
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                if (objectInputStream != null) {
                    try {
                        objectInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            return accountInfo;
        }

        private static byte[] hexStringToByteArray(String hexString) {
            int len = hexString.length();
            byte[] data = new byte[len / 2];
            for (int i = 0; i < len; i += 2) {
                data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4) + Character
                        .digit(hexString.charAt(i + 1), 16));
            }
            return data;
        }

        private void writeObject(ObjectOutputStream out) throws IOException {
            out.writeObject(accountInfo.getPassportInfo());
            out.writeObject(accountInfo.getServiceInfoMap());
        }

        @SuppressWarnings("unchecked")
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            AccountInfo.PassportInfo passportInfo = (AccountInfo.PassportInfo) in.readObject();
            Map<String, AccountInfo.ServiceInfo> serviceInfoMap = (Map<String, AccountInfo.ServiceInfo>) in.readObject();
            accountInfo = AccountInfo.newAccountInfo(passportInfo, serviceInfoMap);
        }

    }

}
