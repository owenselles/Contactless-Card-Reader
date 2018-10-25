package com.owenselles.nfcreader.activity;

import android.Manifest;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.nfc.NfcAdapter;
import android.nfc.cardemulation.CardEmulation;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.owenselles.nfcreader.R;
import com.owenselles.nfcreader.adapter.TabLayoutFragmentPagerAdapter;
import com.owenselles.nfcreader.fragment.PaycardsTabFragment;
import com.owenselles.nfcreader.service.PaymentHostApduService;
import com.owenselles.nfcreader.util.LogUtil;

import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private Dialog mSupportedPaycardsDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtil.d(TAG, "\"" + TAG + "\": Activity create");

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Button customToolbarSupportedPaycardsButton = findViewById(R.id.custom_toolbar_supported_paycards_button);
        customToolbarSupportedPaycardsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSupportedPaycardsDialog = new Dialog(MainActivity.this);
                mSupportedPaycardsDialog.setContentView(R.layout.dialog_supported_paycards);
                mSupportedPaycardsDialog.setCancelable(true);
                mSupportedPaycardsDialog.show();
            }
        });

        ArrayList<TabLayoutFragmentPagerAdapter.ITabLayoutFragmentPagerAdapter> arrayList = new ArrayList<>();
        arrayList.add(new PaycardsTabFragment());

        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(new TabLayoutFragmentPagerAdapter(getSupportFragmentManager(), arrayList));


        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)) {
            ComponentName paymentServiceComponentName = new ComponentName(this, PaymentHostApduService.class);

            CardEmulation cardEmulation = CardEmulation.getInstance(NfcAdapter.getDefaultAdapter(this));
            if (cardEmulation.isDefaultServiceForCategory(paymentServiceComponentName, CardEmulation.CATEGORY_PAYMENT)) {
                LogUtil.i(TAG, "\"" + getString(R.string.app_name) + "\" is default payment application");
            } else {
                LogUtil.i(TAG, "\"" + getString(R.string.app_name) + "\" is not default payment application");

                Intent intent = new Intent();
                intent.setAction(CardEmulation.ACTION_CHANGE_DEFAULT);
                intent.putExtra(CardEmulation.EXTRA_SERVICE_COMPONENT, paymentServiceComponentName);
                intent.putExtra(CardEmulation.EXTRA_CATEGORY, CardEmulation.CATEGORY_PAYMENT);

                startActivity(intent);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        LogUtil.d(TAG, "\"" + TAG + "\": Activity start");

        // Runtime permission(s)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] runtimePermissionsArr = {
                    Manifest.permission.NFC,

                    Manifest.permission.INTERNET,

                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,

                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.CHANGE_NETWORK_STATE,

                    Manifest.permission.ACCESS_FINE_LOCATION, // User interaction needed (not automatically granted, by default denied)
                    Manifest.permission.ACCESS_COARSE_LOCATION, // User interaction needed (not automatically granted, by default denied)

                    Manifest.permission.VIBRATE
            };

            ArrayList<String> requestRuntimePermissionsArrList = new ArrayList<>();

            for (String runtimePermission : runtimePermissionsArr) {
                if (checkSelfPermission(runtimePermission) != PackageManager.PERMISSION_GRANTED) { // checkSelfPermission(runtimePermission) == PackageManager.PERMISSION_DENIED
                    requestRuntimePermissionsArrList.add(runtimePermission);
                }
            }

            if (!requestRuntimePermissionsArrList.isEmpty()) {
                requestPermissions(requestRuntimePermissionsArrList.toArray(new String[requestRuntimePermissionsArrList.size()]), 1);
            }
        }
        // - Runtime permission(s)
    }

    @Override
    protected void onStop() {
        super.onStop();
        LogUtil.d(TAG, "\"" + TAG + "\": Activity stop");

        if (mSupportedPaycardsDialog != null) {
            if (mSupportedPaycardsDialog.isShowing()) {
                mSupportedPaycardsDialog.hide();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtil.d(TAG, "\"" + TAG + "\": Activity destroy");

        if (mSupportedPaycardsDialog != null) {
            mSupportedPaycardsDialog = null;
        }
    }
    public void displayToast(String message) {
        int duration = Toast.LENGTH_LONG;
        final Toast toast = Toast.makeText(getApplicationContext(), message,
                duration);

        toast.show();

        new CountDownTimer(15000, 1000)
        {
            public void onTick(long millisUntilFinished) {
                if (toast.getView().getWindowToken() != null)
                    toast.show();
                else
                    cancel();
            }
            public void onFinish() {
                if (toast.getView().getWindowToken() !=null)
                    toast.show();
                else
                    cancel();
            }

        }.start();
    }
    public void showTerms(View view) {
            displayToast(getString(R.string.terms));
    }
}
