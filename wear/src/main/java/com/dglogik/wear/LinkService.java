package com.dglogik.wear;

import android.app.Service;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import com.dglogik.wear.providers.DeviceProvider;
import com.dglogik.wear.providers.GyroscopeProvider;
import com.dglogik.wear.providers.HealthProvider;
import com.dglogik.wear.providers.ScreenProvider;
import com.dglogik.wear.providers.SpeechProvider;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unchecked")
public class LinkService extends Service {
    public SensorManager sensorManager;
    public GoogleApiClient googleClient;
    public static LinkService INSTANCE;
    private RequestListener reqListener;
    public List<Provider> providers = new ArrayList<>();
    public List<Action> actions = new ArrayList<>();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        INSTANCE = this;

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        Utils.printSensorList(sensorManager);

        googleClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Utils.log("DGWear Connected to the Google API Client");
                        init();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                    }
                })
                .addApi(Wearable.API)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        googleClient.connect();
        Utils.log("Starting DGWear");
        return START_STICKY;
    }

    public <T> T getProviderByClass(Class<? extends Provider> providerClass) {
        for (Provider provider : providers) {
            if (provider.getClass() == providerClass && provider.supported()) {
                return (T) provider;
            }
        }
        return null;
    }

    public Set<String> connected = new HashSet<>();

    public void init() {
        Wearable.NodeApi.addListener(googleClient, new NodeApi.NodeListener() {
            @Override
            public void onPeerConnected(Node node) {
                Utils.log("Connected to Node: " + node.getId() + " (" + node.getDisplayName() + ")");
            }

            @Override
            public void onPeerDisconnected(Node node) {
                Utils.log("Disconnected from Node: " + node.getId() + " (" + node.getDisplayName() + ")");
                connected.remove(node.getId());
            }
        });

        reqListener = new RequestListener();

        Wearable.MessageApi.addListener(googleClient, reqListener);

        actions.add(new Action("StartSpeechRecognition") {
            @Override
            public void invoke() {
                SpeechProvider provider = getProviderByClass(SpeechProvider.class);
                if (provider != null) {
                    provider.recognizer.startListening(new Intent());
                }
            }
        });

        actions.add(new Action("StopSpeechRecognition") {
            @Override
            public void invoke() {
                SpeechProvider provider = getProviderByClass(SpeechProvider.class);
                if (provider != null) {
                    provider.recognizer.stopListening();
                }
            }
        });

        providers.add(new StepsProvider());
        providers.add(new DeviceProvider());
        providers.add(new ScreenProvider());
        providers.add(new HealthProvider());
        providers.add(new SpeechProvider());

        providers.add(new GyroscopeProvider());

        for (Provider provider : providers) {
            if (!provider.supported()) {
                return;
            }

            provider.setup();
            provider.setInitialValues();
        }

        Wearable.NodeApi.getConnectedNodes(googleClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                for (Node node : getConnectedNodesResult.getNodes()) {
                    reqListener.doInit(node.getId());
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        googleClient.disconnect();
        for (Provider provider : providers) {
            provider.destroy();
        }
    }

    public void send(String type, HashMap<String, Object> objects) throws JSONException {
        final JSONObject object = new JSONObject(objects);

        object.put("device", Build.MODEL);
        object.put("type", type);

        for (String id : connected) {
            Wearable.MessageApi.sendMessage(googleClient, id, "/wear", object.toString().getBytes());
        }
    }

    public void sendSingle(String node, String type, HashMap<String, Object> objects) throws JSONException {
        final JSONObject object = new JSONObject(objects);

        object.put("device", Build.MODEL);
        object.put("type", type);

        Wearable.MessageApi.sendMessage(googleClient, node, "/wear", object.toString().getBytes());
    }
}
