package com.owenselles.nfcreader.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.cardemulation.CardEmulation;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import com.owenselles.nfcreader.R;
import com.owenselles.nfcreader.service.PaymentHostApduService;
import com.owenselles.nfcreader.thread.HostPaycardThread;
import com.owenselles.nfcreader.util.HexUtil;
import com.owenselles.nfcreader.util.LogUtil;

public class HostPaycardActivity extends AppCompatActivity {
    private static final String TAG = HostPaycardActivity.class.getSimpleName();

    private byte[] mApplicationPan = null;

    private SharedPreferences mSharedPreferences = null;
    private SharedPreferences.OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener = null;

    private NfcAdapter mNfcAdapter = null;

    private CardEmulation mCardEmulation = null;

    private AlertDialog mAlertDialog = null;

    // Receiver(s)
    // Broadcast intent action(s)
    public static final String ACTION_SUCCESS_HOST_PAYCARD_BROADCAST = TAG + "SuccessBroadcastIntentAction";
    public static final String ACTION_CANNOT_HOST_PAYCARD_BROADCAST = TAG + "CannotBroadcastIntentAction";
    // - Broadcast intent action(s)

    private BroadcastReceiver mSuccessHostPaycardCustomReceiver = null;
    private BroadcastReceiver mCannotHostPaycardCustomReceiver = null;
    // - Receiver(s)

    private void nfcNotSupported() {
        LogUtil.w(TAG, "NFC Not Supported");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("NFC Not Supported");
        builder.setMessage("Your device does not support NFC feature.");
        builder.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();

                finish();
            }
        });
        builder.setCancelable(false);

        mAlertDialog = builder.create();

        mAlertDialog.show();
    }

    private void nfcNotEnabled() {
        LogUtil.w(TAG, "NFC Not Enabled");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("NFC Not Enabled");
        builder.setMessage("Enable NFC feature and come back again.");
        builder.setPositiveButton("Enable NFC", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
            }
        });
        builder.setNegativeButton(getText(R.string.close), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();

                finish();
            }
        });
        builder.setCancelable(false);

        mAlertDialog = builder.create();

        mAlertDialog.show();
    }

    private void nfcHceNotSupported() {
        LogUtil.w(TAG, "NFC HCE Not Supported");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("NFC HCE Not Supported");
        builder.setMessage("Your device does not support NFC host card emulation feature.");
        builder.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();

                finish();
            }
        });
        builder.setCancelable(false);

        mAlertDialog = builder.create();

        mAlertDialog.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtil.d(TAG, "\"" + TAG + "\": Activity create");

        mApplicationPan = getIntent().getByteArrayExtra(getString(R.string.pan_var_name));

        setContentView(R.layout.activity_host_paycard);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        try {
            mSharedPreferences = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }
        try {
            mOnSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                    LogUtil.d(TAG, "Shared preference changed");

                    LogUtil.i(TAG, "Shared preference changed: " + s);

                    // Shared preference changed relative
                    if (sharedPreferences.contains(getString(R.string.pan_var_name))) {
                        LogUtil.i(TAG, getString(R.string.pan_var_name) + ": " + sharedPreferences.getString(getString(R.string.pan_var_name), "N/A"));
                    }

                    if (!sharedPreferences.contains(getString(R.string.pan_var_name)) || !sharedPreferences.getString(getString(R.string.pan_var_name), "N/A").equals(HexUtil.bytesToHexadecimal(mApplicationPan))) {
                        Toast.makeText(HostPaycardActivity.this, getString(R.string.cannot_host_paycard), Toast.LENGTH_LONG).show();

                        finish(false);
                    }
                    // - Shared preference changed relative
                }
            };
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }

        if (mSharedPreferences != null) {
            if (mOnSharedPreferenceChangeListener != null) {
                try {
                    mSharedPreferences.registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
                } catch (Exception e) {
                    LogUtil.e(TAG, e.getMessage());
                    LogUtil.e(TAG, e.toString());

                    e.printStackTrace();
                }
            }

            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putString(getString(R.string.pan_var_name), HexUtil.bytesToHexadecimal(mApplicationPan));
            editor.apply();
        }

        try {
            mSuccessHostPaycardCustomReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    LogUtil.d(TAG, "\"" + TAG + "\": Receiver receive; " + HostPaycardActivity.this.getString(R.string.success_host_paycard));

                    if (context == null || intent == null) {
                        if (context == null) {
                            LogUtil.w(TAG, "\"" + TAG + "\": Receiver context lack; " + HostPaycardActivity.this.getString(R.string.success_host_paycard));
                        }

                        if (intent == null) {
                            LogUtil.w(TAG, "\"" + TAG + "\": Receiver intent lack; " + HostPaycardActivity.this.getString(R.string.success_host_paycard));
                        }

                        return;
                    }

                    String intentAction = null;
                    try {
                        intentAction = intent.getAction();
                    } catch (Exception e) {
                        LogUtil.e(TAG, e.getMessage());
                        LogUtil.e(TAG, e.toString());

                        e.printStackTrace();
                    }

                    if (intentAction == null || !intentAction.equals(ACTION_SUCCESS_HOST_PAYCARD_BROADCAST)) {
                        if (intentAction == null) {
                            LogUtil.w(TAG, "\"" + TAG + "\": Receiver intent action lack; " + HostPaycardActivity.this.getString(R.string.success_host_paycard));
                        } else if (!intentAction.equals(ACTION_SUCCESS_HOST_PAYCARD_BROADCAST)) {
                            LogUtil.w(TAG, "\"" + TAG + "\": Receiver intent action mismatch ; " + HostPaycardActivity.this.getString(R.string.success_host_paycard));
                        }

                        return;
                    }

                    successHostPaycardReceiverOk(context, intent);
                }
            };
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }
        try {
            mCannotHostPaycardCustomReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    LogUtil.d(TAG, "\"" + TAG + "\": Receiver receive; " + HostPaycardActivity.this.getString(R.string.cannot_host_paycard));

                    if (context == null || intent == null) {
                        if (context == null) {
                            LogUtil.w(TAG, "\"" + TAG + "\": Receiver context lack; " + HostPaycardActivity.this.getString(R.string.cannot_host_paycard));
                        }

                        if (intent == null) {
                            LogUtil.w(TAG, "\"" + TAG + "\": Receiver intent lack; " + HostPaycardActivity.this.getString(R.string.cannot_host_paycard));
                        }

                        return;
                    }

                    String intentAction = null;
                    try {
                        intentAction = intent.getAction();
                    } catch (Exception e) {
                        LogUtil.e(TAG, e.getMessage());
                        LogUtil.e(TAG, e.toString());

                        e.printStackTrace();
                    }

                    if (intentAction == null || !intentAction.equals(ACTION_CANNOT_HOST_PAYCARD_BROADCAST)) {
                        if (intentAction == null) {
                            LogUtil.w(TAG, "\"" + TAG + "\": Receiver intent action lack; " + HostPaycardActivity.this.getString(R.string.cannot_host_paycard));
                        } else if (!intentAction.equals(ACTION_CANNOT_HOST_PAYCARD_BROADCAST)) {
                            LogUtil.w(TAG, "\"" + TAG + "\": Receiver intent action mismatch; " + HostPaycardActivity.this.getString(R.string.cannot_host_paycard));
                        }

                        return;
                    }

                    cannotHostPaycardReceiverOk(context, intent);
                }
            };
        } catch (Exception e) {
            LogUtil.e(TAG, e.getMessage());
            LogUtil.e(TAG, e.toString());

            e.printStackTrace();
        }

        runOnUiThread(new HostPaycardThread(this));
    }

    @Override
    protected void onStart() {
        super.onStart();
        LogUtil.d(TAG, "\"" + TAG + "\": Activity start");

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC)) {
            // Try to get the NFC adapter directly
            try {
                mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
            } catch (Exception e) {
                LogUtil.e(TAG, e.getMessage());
                LogUtil.e(TAG, e.toString());

                e.printStackTrace();
            }

            if (mNfcAdapter == null) {
                NfcManager nfcManager = null;
                try {
                    nfcManager = (NfcManager) getSystemService(NFC_SERVICE);
                } catch (Exception e) {
                    LogUtil.e(TAG, e.getMessage());
                    LogUtil.e(TAG, e.toString());

                    e.printStackTrace();
                }

                if (nfcManager != null) {
                    // Try to get the NFC adapter alternatively
                    try {
                        mNfcAdapter = nfcManager.getDefaultAdapter();
                    } catch (Exception e) {
                        LogUtil.e(TAG, e.getMessage());
                        LogUtil.e(TAG, e.toString());

                        e.printStackTrace();
                    }
                    // - Try to get the NFC adapter alternatively
                }
            }

            if (mNfcAdapter == null) {
                nfcNotSupported();
            } else if (!mNfcAdapter.isEnabled()) {
                nfcNotEnabled();
            } else if (mNfcAdapter.isEnabled()) {
                // Activity relative
                try {
                    mCardEmulation = CardEmulation.getInstance(mNfcAdapter);
                } catch (Exception e) {
                    LogUtil.e(TAG, e.getMessage());
                    LogUtil.e(TAG, e.toString());

                    e.printStackTrace();
                }
                // - Activity relative
            }
        } else {
            nfcNotSupported();
        }

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)) {
            // Activity relative
            // Register host paycard receiver(s) from thread
            if (mSuccessHostPaycardCustomReceiver != null) {
                LocalBroadcastManager.getInstance(this)
                        .registerReceiver(mSuccessHostPaycardCustomReceiver, new IntentFilter(ACTION_SUCCESS_HOST_PAYCARD_BROADCAST)
                        ); // Success host paycard
            }
            if (mCannotHostPaycardCustomReceiver != null) {
                LocalBroadcastManager.getInstance(this)
                        .registerReceiver(mCannotHostPaycardCustomReceiver, new IntentFilter(ACTION_CANNOT_HOST_PAYCARD_BROADCAST)
                        ); // Cannot host paycard
            }
            // - Register host paycard receiver(s) from thread
            // - Activity relative
        } else {
            nfcHceNotSupported();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LogUtil.d(TAG, "\"" + TAG + "\": Activity resume");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (mCardEmulation != null && mCardEmulation.categoryAllowsForegroundPreference(CardEmulation.CATEGORY_PAYMENT)) {
                mCardEmulation.setPreferredService(this, new ComponentName(this, PaymentHostApduService.class));
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LogUtil.d(TAG, "\"" + TAG + "\": Activity pause");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (mCardEmulation != null && mCardEmulation.categoryAllowsForegroundPreference(CardEmulation.CATEGORY_PAYMENT)) {
                mCardEmulation.unsetPreferredService(this);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        LogUtil.d(TAG, "\"" + TAG + "\": Activity stop");

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC)) {
            if (mNfcAdapter != null) {
                if (mNfcAdapter.isEnabled()) {
                    // Activity relative
                    // Unregister (deregister) read paycard receiver(s) from thread
                    if (mCannotHostPaycardCustomReceiver != null) {
                        LocalBroadcastManager.getInstance(this)
                                .unregisterReceiver(mCannotHostPaycardCustomReceiver);
                    }
                    if (mSuccessHostPaycardCustomReceiver != null) {
                        LocalBroadcastManager.getInstance(this)
                                .unregisterReceiver(mSuccessHostPaycardCustomReceiver);
                    }
                    // - Unregister (deregister) read paycard receiver(s) from thread
                    // - Activity relative
                }
            }
        }

        if (mAlertDialog != null) {
            if (mAlertDialog.isShowing()) {
                mAlertDialog.dismiss();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtil.d(TAG, "\"" + TAG + "\": Activity destroy");

        if (mCannotHostPaycardCustomReceiver != null) {
            mCannotHostPaycardCustomReceiver = null;
        }
        if (mSuccessHostPaycardCustomReceiver != null) {
            mSuccessHostPaycardCustomReceiver = null;
        }

        if (mAlertDialog != null) {
            mAlertDialog = null;
        }

        if (mCardEmulation != null) {
            mCardEmulation = null;
        }

        if (mNfcAdapter != null) {
            mNfcAdapter = null;
        }

        if (mSharedPreferences != null) {
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            // editor.putString(getString(R.string.pan_var_name), "N/A");
            editor.remove(getString(R.string.pan_var_name));
            editor.apply();

            if (mOnSharedPreferenceChangeListener != null) {
                try {
                    mSharedPreferences.unregisterOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
                } catch (Exception e) {
                    LogUtil.e(TAG, e.getMessage());
                    LogUtil.e(TAG, e.toString());

                    e.printStackTrace();
                }
            }
        }

        if (mOnSharedPreferenceChangeListener != null) {
            mOnSharedPreferenceChangeListener = null;
        }
        if (mSharedPreferences != null) {
            mSharedPreferences = null;
        }

        if (mApplicationPan != null) {
            mApplicationPan = null;
        }

        System.gc(); // All done, recycle unused objects (mainly because of thread)
    }

    public void successHostPaycardReceiverOk(@NonNull Context context, @NonNull Intent intent) {
        LogUtil.d(TAG, "\"" + TAG + "\": Receiver receive OK; " + getString(R.string.success_host_paycard));

        // Incoming context is from the receiver; using activity context is better option here

        // Receiver Relative
        Toast.makeText(this, getString(R.string.success_host_paycard), Toast.LENGTH_LONG).show();

        finish(false);
        // - Receiver Relative
    }
    public void cannotHostPaycardReceiverOk(@NonNull Context context, @NonNull Intent intent) {
        LogUtil.d(TAG, "\"" + TAG + "\": Receiver receive OK; " + getString(R.string.cannot_host_paycard));

        // Incoming context is from the receiver; using activity context is better option here

        // Receiver Relative
        Toast.makeText(this, getString(R.string.cannot_host_paycard), Toast.LENGTH_LONG).show();

        finish(false);
        // - Receiver Relative
    }

    private void finish(boolean andRemoveTask) {
        if (andRemoveTask) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                finishAndRemoveTask();
            }

            return;
        }

        finish();
    }
}
