package com.owenselles.nfcreader.thread;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import com.owenselles.nfcreader.R;
import com.owenselles.nfcreader.activity.HostPaycardActivity;
import com.owenselles.nfcreader.envr.MainEnvr;
import com.owenselles.nfcreader.object.PaycardObject;
import com.owenselles.nfcreader.util.HexUtil;
import com.owenselles.nfcreader.util.KeyUtil;
import com.owenselles.nfcreader.util.LogUtil;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class HostPaycardThread implements Runnable {
    private static final String TAG = HostPaycardThread.class.getSimpleName();

    private PaycardObject mPaycardObject = null;

    private Context mContext;

    public HostPaycardThread(@NonNull Context context) {
        mContext = context;

        Vibrator vibrator = null;
        try {
            vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }

        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    vibrator.vibrate(VibrationEffect.createOneShot(MainEnvr.HOST_PAYCARD_VIBE_TIME, VibrationEffect.DEFAULT_AMPLITUDE));
                } catch (Exception e) {
                    LogUtil.e(TAG, e.getMessage());
                    LogUtil.e(TAG, e.toString());

                    e.printStackTrace();
                }
            } else {
                try {
                    vibrator.vibrate(MainEnvr.HOST_PAYCARD_VIBE_TIME);
                } catch (Exception e) {
                    LogUtil.e(TAG, e.getMessage());
                    LogUtil.e(TAG, e.toString());

                    e.printStackTrace();
                }
            }
        }
    }

    private void successHostPaycard() {
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(HostPaycardActivity.ACTION_SUCCESS_HOST_PAYCARD_BROADCAST));
    }

    private void cannotHostPaycard() {
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(HostPaycardActivity.ACTION_CANNOT_HOST_PAYCARD_BROADCAST));
    }

    @Override
    public void run() {
        LogUtil.d(TAG, "\"" + TAG + "\": Thread run");

        // Thread relative
        // Get encryption key
        byte[] getEncryptionKey = KeyUtil.getEncryptionKey(mContext);
        // - Get encryption key

        Realm realm = null;
        if (getEncryptionKey != null) {
            RealmConfiguration realmConfiguration = new RealmConfiguration.Builder()
                    .encryptionKey(getEncryptionKey)
                    .build();

            try {
                realm = Realm.getInstance(realmConfiguration);
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }
        } else {
            try {
                realm = Realm.getDefaultInstance();
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }
        }

        if (realm != null) {
            byte[] applicationPan = null;

            SharedPreferences sharedPreferences = mContext.getSharedPreferences(mContext.getString(R.string.app_name), Context.MODE_PRIVATE);
            String applicationPanHexadecimal = sharedPreferences.getString(mContext.getString(R.string.pan_var_name), "N/A");

            if (!applicationPanHexadecimal.equals("N/A")) {
                applicationPan = HexUtil.hexadecimalToBytes(applicationPanHexadecimal);

                if (applicationPan != null) {
                    try {
                        mPaycardObject = realm.where(PaycardObject.class).equalTo(mContext.getString(R.string.pan_var_name), applicationPan).findFirst();
                    } catch (Exception e) {
                        LogUtil.e(TAG, e.getMessage());
                        LogUtil.e(TAG, e.toString());

                        e.printStackTrace();
                    }
                }
            }
        }

        if (realm == null || mPaycardObject == null) {
            if (realm == null) {
                LogUtil.w(TAG, "Realm is null");
            }
            if (mPaycardObject == null) {
                LogUtil.w(TAG, "PaycardObject is null");
            }

            this.cannotHostPaycard();
            return;
        }
        // - Thread relative

        // Finalize
        successHostPaycard();
        // - Finalize
    }
}
