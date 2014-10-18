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
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONObject;

import java.util.List;

public class DGMobileContext {
    public static final String TAG = "DGWear";
    public static DGMobileContext CONTEXT;

    public final MainActivity mainActivity;
    public final WearableSupport wearable;
    public final GoogleApiClient googleClient;
    public final Application link;
    public boolean linkStarted = false;
    public final RootNode rootNode;

    public DGMobileContext(final MainActivity mainActivity) {
        CONTEXT = this;
        this.mainActivity = mainActivity;
        this.rootNode = new RootNode();
        this.wearable = new WearableSupport(this);
        this.googleClient = new GoogleApiClient.Builder(getApplicationContext())
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.i(TAG, "Google API Client Connected");

                        Wearable.NodeApi.getConnectedNodes(googleClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                            @Override
                            public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                                List<Node> nodes = getConnectedNodesResult.getNodes();

                                for (Node node : nodes) {
                                    Wearable.MessageApi.sendMessage(googleClient, node.getId(), "/wear/init", null);
                                }
                            }
                        });
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Starting Link");

                linkStarted = true;

                link.addRootNode(rootNode);
                link.run(new String[0], false);
            }
        }).start();
    }

    public Context getApplicationContext() {
        return mainActivity.getApplicationContext();
    }
}
