package com.dglogik.mobile.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class ScanBarcodeActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        ResultReceiver receiver = (ResultReceiver) getIntent().getExtras().get("receiver");

        Runnable done = new Runnable() {
            @Override
            public void run() {
                finish();
            }
        };

        Bundle bundle = new Bundle();

        if (scanResult == null) {
            bundle.putString("code", "");
            bundle.putString("format", "");
            if (receiver != null) {
                receiver.send(RESULT_OK, bundle);
            }
            done.run();
            return;
        }

        bundle.putString("code", scanResult.getContents());
        bundle.putString("format", scanResult.getFormatName());
        if (receiver != null) {
            receiver.send(RESULT_OK, bundle);
        }

        done.run();
    }
}
