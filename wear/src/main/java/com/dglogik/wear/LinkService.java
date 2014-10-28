package com.dglogik.wear;

import android.app.Service;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import com.dglogik.wear.providers.DeviceProvider;
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
import java.util.List;

@SuppressWarnings("unchecked")
public class LinkService extends Service {
    public SensorManager sensorManager;
    public GoogleApiClient googleClient;
    public static LinkService INSTANCE;
    public List<Provider> providers = new ArrayList<Provider>();
    public List<Action> actions = new ArrayList<Action>();

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

    public void init() {
        Wearable.MessageApi.addListener(googleClient, new RequestListener());

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

        /* TODO: Uncomment the following line when Gyroscope is ready */
        //providers.add(new GyroscopeProvider());

        for (Provider provider : providers) {
            if (!provider.supported()) {
                return;
            }

            provider.setup();
        }
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

        object.put("device", Build.MODEL);
        object.put("type", type);

        Wearable.MessageApi.sendMessage(googleClient, node, "/wear", object.toString().getBytes());
    }
}
