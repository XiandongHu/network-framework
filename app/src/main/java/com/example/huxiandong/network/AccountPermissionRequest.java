package com.example.huxiandong.network;

import android.app.Activity;

import com.xiaomi.accounts.Manifest;
import com.xiaomi.passport.utils.RuntimePermissionActor;
import com.xiaomi.passport.utils.RuntimePermissionHelper;

/**
 * Created by huxiandong
 * on 17-4-1.
 */

class AccountPermissionRequest {

    private final RuntimePermissionHelper mPermissionHelper;

    AccountPermissionRequest(Activity activity, Runnable grantRunnable) {
        mPermissionHelper = new RuntimePermissionHelper.Builder()
                .actor(new RuntimePermissionActor.Builder()
                        .activity(activity)
                        .requestCode(1)
                        .requestedPermission(Manifest.permission.GET_ACCOUNTS)
                        .explainDialogOkText(android.R.string.ok)
                        .explainDialogCancelText(android.R.string.cancel)
                        .explainDialogTitle(R.string.get_account_dialog_title)
                        .explainDialogMessage(R.string.get_account_dialog_message)
                        .build())
                .runnableIfDenied(new Runnable() {
                    @Override
                    public void run() {
                    }
                })
                .runnableIfGranted(grantRunnable)
                .build();
    }

    void checkAndRun() {
        mPermissionHelper.checkAndRun();
    }

    void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        mPermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}
