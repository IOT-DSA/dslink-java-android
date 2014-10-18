package com.dglogik.wear;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

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
    private GoogleApiClient googleClient;
    private SensorManager sensorManager;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

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
        List<Sensor> allSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);

        Sensor healthSensor = null;

        final List<String> names = new ArrayList<String>();

        names.add("Health");

        for (Sensor sensor : allSensors) {
            System.out.println(sensor.getName());
            if (sensor.getName().equals("Wellness Passive Sensor")) {
                healthSensor = sensor;
                break;
            }
        }

        if (healthSensor == null) {
            return;
        }

        Wearable.NodeApi.addListener(googleClient, new NodeApi.NodeListener() {
            @Override
            public void onPeerConnected(Node node) {
                System.out.println("Node Connected: " + node.getDisplayName() + " (ID: " + node.getId() + ")");
                try {
                    sendSingle(node, "sensors", new HashMap<String, Object>() {{
                        put("sensors", names);
                    }});
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onPeerDisconnected(Node node) {
                System.out.println("Node Disconnected: " + node.getDisplayName() + " (ID: " + node.getId() + ")");
            }
        });

        sensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(final SensorEvent sensorEvent) {
                try {
                    send("update", new HashMap<String, Object>() {{
                        put("value", ((double) sensorEvent.values[2]));
                        put("node", "Health");
                    }});
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        }, healthSensor, SensorManager.SENSOR_DELAY_NORMAL);
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

    public void sendSingle(Node node, String type, HashMap<String, Object> objects) throws JSONException {
        final JSONObject object = new JSONObject(objects);

        object.put("type", type);

        Wearable.MessageApi.sendMessage(googleClient, node.getId(), "/wear", object.toString().getBytes());
    }
}
