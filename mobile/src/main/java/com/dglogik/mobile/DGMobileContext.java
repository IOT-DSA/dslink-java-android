package com.dglogik.mobile;

import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import com.dglogik.dslink.Application;
import com.dglogik.mobile.link.RootNode;
import com.dglogik.mobile.wear.WearableSupport;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

public class DGMobileContext {
    public static final String TAG = "DGWear";
    public static DGMobileContext CONTEXT;

    public final MainActivity mainActivity;
    public final WearableSupport wearable;
    public final GoogleApiClient googleClient;
    public final Application link;

    public DGMobileContext(final MainActivity mainActivity) {
        CONTEXT = this;
        this.mainActivity = mainActivity;
        this.wearable = new WearableSupport(this);
        this.googleClient = new GoogleApiClient.Builder(getApplicationContext())
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.i(TAG, "Google API Client Connected");
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        Log.e(TAG, "Google API Client Connection Failed!");
                    }
                })
                .addApi(Wearable.API)
                .build();
        link = new Application();
    }

    public void initialize() {
        wearable.initialize();
        googleClient.connect();
    }

    public void startLink() {
        Runnable action = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                link.addRootNode(new RootNode());

                link.run(new String[0], false);
            }
        };

        Thread thread = new Thread(action);
        thread.start();
    }

    public Context getApplicationContext() {
        return mainActivity.getApplicationContext();
    }
}
