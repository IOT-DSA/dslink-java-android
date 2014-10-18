package com.dglogik.wear;

import android.app.Activity;
import android.hardware.SensorManager;
import android.os.Bundle;

import com.dglogik.wear.providers.DeviceProvider;
import com.dglogik.wear.providers.GyroscopeProvider;
import com.dglogik.wear.providers.ScreenProvider;
import com.dglogik.wear.providers.StepsProvider;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends Activity {
    public GoogleApiClient googleClient;
    public static MainActivity INSTANCE;
    public SensorManager sensorManager;
    public List<Provider> providers = new ArrayList<Provider>();

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        INSTANCE = this;

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        googleClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        System.out.println("DGWear Connected to the Google API Client");
                        init();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                    }
                })
                .addApi(Wearable.API)
                .build();

        googleClient.connect();

        System.out.println("Starting DGWear");
    }

    public void init() {
        Wearable.MessageApi.addListener(googleClient, new RequestListener());

        providers.add(new StepsProvider());
        providers.add(new DeviceProvider());
        providers.add(new ScreenProvider());

        /* TODO: Uncomment the following line when Gyroscope is ready */
        //providers.add(new GyroscopeProvider());

        for (Provider provider : providers) {
            if (!provider.supported()) {
                return;
            }

            provider.setup();
        }
    }

    public void send(String type, HashMap<String, Object> objects) throws JSONException {
        final JSONObject object = new JSONObject(objects);

        object.put("type", type);

        Wearable.NodeApi.getConnectedNodes(googleClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult result) {
                for (Node node : result.getNodes()) {
                    Wearable.MessageApi.sendMessage(googleClient, node.getId(), "/wear", object.toString().getBytes());
                }
            }
        });
    }

    public void sendSingle(String node, String type, HashMap<String, Object> objects) throws JSONException {
        final JSONObject object = new JSONObject(objects);

        object.put("type", type);

        Wearable.MessageApi.sendMessage(googleClient, node, "/wear", object.toString().getBytes());
    }
}
