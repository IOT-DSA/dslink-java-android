package com.dglogik.mobile;

import android.content.Context;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.os.Build;

import com.dglogik.api.BasicMetaData;
import com.dglogik.dslink.Application;
import com.dglogik.mobile.link.RootNode;
import com.dglogik.mobile.wear.WearableSupport;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.*;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import com.google.android.gms.wearable.Wearable;

import com.dglogik.mobile.link.*;

import android.location.*;

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
    public final SensorManager sensorManager;
    public final LocationManager locationManager;

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

                        initialize();

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
                .addApi(LocationServices.API)
                .build();

        googleClient.connect();

        this.sensorManager = (SensorManager) mainActivity.getSystemService(MainActivity.SENSOR_SERVICE);
        this.locationManager = (LocationManager) mainActivity.getSystemService(MainActivity.LOCATION_SERVICE);
        link = new Application();
    }

    public void initialize() {
        wearable.initialize();

        DeviceNode device = new DeviceNode(Build.MODEL);

        setupCurrentDevice(device);

        rootNode.addChild(device);
    }

    public void setupCurrentDevice(DeviceNode node) {
        {
            final DataValueNode latitudeNode = new DataValueNode("Location_Latitude", BasicMetaData.SIMPLE_INT);
            final DataValueNode longitudeNode = new DataValueNode("Location_Longitude", BasicMetaData.SIMPLE_INT);

            LocationRequest request = new LocationRequest();

            request.setFastestInterval(250);
            request.setInterval(1000);

            LocationServices.FusedLocationApi.requestLocationUpdates(googleClient, request, new com.google.android.gms.location.LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    latitudeNode.update(location.getLatitude());
                    longitudeNode.update(location.getLongitude());
                }
            });

            node.addChild(latitudeNode);
            node.addChild(longitudeNode);
        }
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

    public void onCreate() {
    }
}
