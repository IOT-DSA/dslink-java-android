package com.dglogik.mobile;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Display;

import com.dglogik.api.BasicMetaData;
import com.dglogik.dslink.Application;
import com.dglogik.dslink.client.Client;
import com.dglogik.dslink.node.base.BaseAction;
import com.dglogik.dslink.node.base.BaseNode;
import com.dglogik.mobile.link.DataValueNode;
import com.dglogik.mobile.link.DeviceNode;
import com.dglogik.mobile.link.RootNode;
import com.dglogik.mobile.wear.WearableSupport;
import com.dglogik.value.DGValue;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class DGMobileContext {
    public static final String TAG = "DGMobile";
    public static DGMobileContext CONTEXT;

    @NonNull
    public final LinkService service;
    @NonNull
    public final WearableSupport wearable;
    public final GoogleApiClient googleClient;
    @NonNull
    public final Application link;
    public boolean linkStarted = false;
    @NonNull
    public final RootNode rootNode;
    @NonNull
    public final SensorManager sensorManager;
    @NonNull
    public final LocationManager locationManager;
    @NonNull
    public final Client client;
    public final SharedPreferences preferences;

    public DGMobileContext(@NonNull final LinkService service) {
        CONTEXT = this;
        this.service = service;
        this.rootNode = new RootNode();
        this.wearable = new WearableSupport(this);
        this.preferences = service.getSharedPreferences("settings", Context.MODE_PRIVATE);
        this.googleClient = new GoogleApiClient.Builder(getApplicationContext())
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.i(TAG, "Google API Client Connected");

                        initialize();

                        Wearable.NodeApi.getConnectedNodes(googleClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                            @Override
                            public void onResult(@NonNull NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
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

        this.sensorManager = (SensorManager) service.getSystemService(LinkService.SENSOR_SERVICE);
        this.locationManager = (LocationManager) service.getSystemService(LinkService.LOCATION_SERVICE);
        this.link = new Application();

        this.client = new Client(false) {
            @Override
            protected void onStop() {
                link.stop();
            }
        };
        this.handler = new Handler(getApplicationContext().getMainLooper());

        link.setClient(client);
    }

    public void initialize() {
        wearable.initialize();

        DeviceNode device = new DeviceNode(Build.MODEL);
        setupCurrentDevice(device);
        rootNode.addChild(device);

        startLink();
    }

    public final Timer timer = new Timer();

    public double lastLatitude;
    public double lastLongitude;

    public List<Action> cleanups = new ArrayList<Action>();
    public List<SensorEventListener> sensorListeners = new ArrayList<SensorEventListener>();

    public void onCleanup(Action action) {
        cleanups.add(action);
    }

    public SensorEventListener sensorEventListener(SensorEventListener eventListener) {
        sensorListeners.add(eventListener);
        return eventListener;
    }

    public void setupCurrentDevice(@NonNull DeviceNode node) {
        if (preferences.getBoolean("providers.location", true)) {
            final DataValueNode latitudeNode = new DataValueNode("Location_Latitude", BasicMetaData.SIMPLE_INT);
            final DataValueNode longitudeNode = new DataValueNode("Location_Longitude", BasicMetaData.SIMPLE_INT);

            LocationRequest request = new LocationRequest();

            request.setFastestInterval(1000);
            request.setInterval(3000);
            request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            final LocationListener listener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    if (lastLatitude != location.getLatitude()) {
                        latitudeNode.update(location.getLatitude());
                        lastLatitude = location.getLatitude();
                    }

                    if (lastLongitude != location.getLongitude()) {
                        longitudeNode.update(location.getLongitude());
                        lastLongitude = location.getLatitude();
                    }
                }
            };

            LocationServices.FusedLocationApi.requestLocationUpdates(googleClient, request, listener);

            onCleanup(new Action() {
                @Override
                public void run() {
                    LocationServices.FusedLocationApi.removeLocationUpdates(googleClient, listener);
                }
            });

            node.addChild(latitudeNode);
            node.addChild(longitudeNode);
        }

        if (preferences.getBoolean("providers.battery", true)) {
            final DataValueNode batteryLevelNode = new DataValueNode("Battery_Level", BasicMetaData.SIMPLE_INT);
            final DataValueNode chargerConnectedNode = new DataValueNode("Charger_Connected", BasicMetaData.SIMPLE_BOOL);
            final DataValueNode batteryFullNode = new DataValueNode("Battery_Full", BasicMetaData.SIMPLE_BOOL);
            final Intent batteryStatus = getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

            if (batteryStatus != null) {
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

                        boolean isChargerConnected = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
                        boolean isFull = status == BatteryManager.BATTERY_STATUS_FULL;

                        double percent = (level / (float) scale) * 100;

                        batteryLevelNode.update(percent);
                        chargerConnectedNode.update(isChargerConnected);
                        batteryFullNode.update(isFull);
                    }
                }, 100, 4000);

                node.addChild(batteryLevelNode);
                node.addChild(batteryFullNode);
                node.addChild(chargerConnectedNode);
            }
        }

        if (Build.VERSION.SDK_INT >= 20 && preferences.getBoolean("providers.screen", true)) {
            setupScreenProvider(node);
        }

        if (preferences.getBoolean("actions.notifications", true)) {
            final NotificationManager notificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);

            final BaseAction createNotificationAction = new BaseAction("CreateNotification") {
                @NonNull
                @Override
                public Map<String, DGValue> invoke(BaseNode baseNode, @NonNull Map<String, DGValue> args) {
                    Notification.Builder builder = new Notification.Builder(getApplicationContext());

                    builder.setContentTitle(args.get("title").toString());
                    builder.setContentText(args.get("content").toString());
                    builder.setSmallIcon(R.drawable.ic_launcher);

                    Notification notification = builder.build();

                    currentNotificationId++;

                    notificationManager.notify(currentNotificationId, notification);

                    return new HashMap<String, DGValue>() {{
                        put("id", DGValue.make(currentNotificationId));
                    }};
                }
            };

            createNotificationAction.addParam("title", BasicMetaData.SIMPLE_STRING);
            createNotificationAction.addParam("content", BasicMetaData.SIMPLE_STRING);
            createNotificationAction.addResult("id", BasicMetaData.SIMPLE_INT);

            final BaseAction destroyNotificationAction = new BaseAction("DestroyNotification") {
                @NonNull
                @Override
                public Map<String, DGValue> invoke(BaseNode baseNode, @NonNull Map<String, DGValue> args) {
                    int id = args.get("id").toInt();

                    notificationManager.cancel(id);

                    return new HashMap<String, DGValue>();
                }
            };

            destroyNotificationAction.addParam("id", BasicMetaData.SIMPLE_INT);

            onCleanup(new Action() {
                @Override
                public void run() {
                    notificationManager.cancelAll();
                }
            });

            node.addAction(createNotificationAction);
            node.addAction(destroyNotificationAction);
        }

        if (enableSensor(19)) {
            final DataValueNode stepsNode = new DataValueNode("Steps", BasicMetaData.SIMPLE_INT);
            Sensor sensor = sensorManager.getDefaultSensor(19);

            sensorManager.registerListener(sensorEventListener(new SensorEventListener() {
                @Override
                public void onSensorChanged(@NonNull SensorEvent event) {
                    stepsNode.update((double) event.values[0]);
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }
            }), sensor, SensorManager.SENSOR_DELAY_NORMAL);
            node.addChild(stepsNode);
        }

        if (Build.VERSION.SDK_INT >= 20) {
            setupHeartRateMonitor(node);
        }

        if (enableSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)) {
            final DataValueNode tempCNode = new DataValueNode("Ambient_Temperature_Celsius", BasicMetaData.SIMPLE_INT);
            final DataValueNode tempFNode = new DataValueNode("Ambient_Temperature_Fahrenheit", BasicMetaData.SIMPLE_INT);

            Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);

            sensorManager.registerListener(sensorEventListener(new SensorEventListener() {
                @Override
                public void onSensorChanged(@NonNull SensorEvent event) {
                    double celsius = (double) event.values[0];
                    double fahrenheit = 32 + (celsius * 9 / 5);
                    tempCNode.update(celsius);
                    tempFNode.update(fahrenheit);
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }
            }), sensor, SensorManager.SENSOR_DELAY_NORMAL);
            node.addChild(tempCNode);
            node.addChild(tempFNode);
        }

        //noinspection PointlessBooleanExpression,ConstantConditions
        if (enableSensor(Sensor.TYPE_LIGHT) && Settings.ENABLE_LIGHT_LEVEL) {
            final DataValueNode lux = new DataValueNode("Light_Level", BasicMetaData.SIMPLE_INT);

            Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

            sensorManager.registerListener(sensorEventListener(new SensorEventListener() {
                @Override
                public void onSensorChanged(@NonNull SensorEvent event) {
                    lux.update((double) event.values[0]);
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }
            }), sensor, 6000);
            node.addChild(lux);
        }

        if (enableSensor(Sensor.TYPE_PRESSURE)) {
            final DataValueNode pressure = new DataValueNode("Air_Pressure", BasicMetaData.SIMPLE_INT);

            Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);

            sensorManager.registerListener(sensorEventListener(new SensorEventListener() {
                @Override
                public void onSensorChanged(@NonNull SensorEvent event) {
                    pressure.update((double) event.values[0]);
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }
            }), sensor, SensorManager.SENSOR_DELAY_NORMAL);
            node.addChild(pressure);
        }

        if (enableSensor(Sensor.TYPE_RELATIVE_HUMIDITY)) {
            final DataValueNode humidity = new DataValueNode("Humidity", BasicMetaData.SIMPLE_INT);

            Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);

            sensorManager.registerListener(sensorEventListener(new SensorEventListener() {
                @Override
                public void onSensorChanged(@NonNull SensorEvent event) {
                    humidity.update((double) event.values[0]);
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }
            }), sensor, SensorManager.SENSOR_DELAY_NORMAL);
            node.addChild(humidity);
        }

        if (enableSensor(Sensor.TYPE_PROXIMITY)) {
            final DataValueNode proximity = new DataValueNode("Proximity", BasicMetaData.SIMPLE_INT);

            Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

            sensorManager.registerListener(sensorEventListener(new SensorEventListener() {
                @Override
                public void onSensorChanged(@NonNull SensorEvent event) {
                    proximity.update((double) event.values[0]);
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }
            }), sensor, SensorManager.SENSOR_DELAY_NORMAL);
            node.addChild(proximity);
        }

        //noinspection ConstantConditions,PointlessBooleanExpression
        if (enableSensor(Sensor.TYPE_GYROSCOPE) && Settings.ENABLE_GYROSCOPE) {
            final DataValueNode x = new DataValueNode("Gyroscope_X", BasicMetaData.SIMPLE_INT);
            final DataValueNode y = new DataValueNode("Gyroscope_Y", BasicMetaData.SIMPLE_INT);
            final DataValueNode z = new DataValueNode("Gyroscope_Z", BasicMetaData.SIMPLE_INT);

            Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

            sensorManager.registerListener(sensorEventListener(new SensorEventListener() {
                @Override
                public void onSensorChanged(@NonNull SensorEvent event) {
                    x.update((double) event.values[0]);
                    y.update((double) event.values[1]);
                    z.update((double) event.values[2]);
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }
            }), sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        if (preferences.getBoolean("actions.speak", true)) {
            final TextToSpeech speech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                }
            });

            final BaseAction speakAction = new BaseAction("Speak") {
                @SuppressWarnings("deprecation")
                @NonNull
                @Override
                public Map<String, DGValue> invoke(BaseNode baseNode, @NonNull Map<String, DGValue> args) {
                    speech.speak(args.get("text").toString(), TextToSpeech.QUEUE_ADD, new HashMap<String, String>());
                    return new HashMap<String, DGValue>();
                }
            };

            speakAction.addParam("text", BasicMetaData.SIMPLE_STRING);

            onCleanup(new Action() {
                @Override
                public void run() {
                    speech.stop();
                }
            });

            node.addAction(speakAction);
        }

        if (preferences.getBoolean("providers.speech", true)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());

            final DataValueNode lastSpeechNode = new DataValueNode("Recognized_Speech", BasicMetaData.SIMPLE_STRING);

            final BaseAction startSpeechRecognitionAction = new BaseAction("StartSpeechRecognition") {
                @SuppressWarnings("deprecation")
                @NonNull
                @Override
                public Map<String, DGValue> invoke(BaseNode baseNode, @NonNull Map<String, DGValue> args) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            recognizer.startListening(new Intent());
                        }
                    });
                    return new HashMap<String, DGValue>();
                }
            };

            final BaseAction stopSpeechRecognitionAction = new BaseAction("StopSpeechRecognition") {
                @SuppressWarnings("deprecation")
                @NonNull
                @Override
                public Map<String, DGValue> invoke(BaseNode baseNode, @NonNull Map<String, DGValue> args) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            recognizer.stopListening();
                        }
                    });
                    return new HashMap<String, DGValue>();
                }
            };

            recognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    log("Ready for Speech");
                }

                @Override
                public void onBeginningOfSpeech() {
                    log("Beginning of Speech");
                }

                @Override
                public void onRmsChanged(float rmsdB) {

                }

                @Override
                public void onBufferReceived(byte[] buffer) {

                }

                @Override
                public void onEndOfSpeech() {
                    log("End of Speech");
                }

                @Override
                public void onError(int error) {
                    log("Speech Error");
                }

                @Override
                public void onResults(Bundle results) {
                    log("Speech Results");
                    List<Float> scores = new ArrayList<Float>();
                    List<String> possibles = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    {
                        float[] sc = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
                        for (float score : sc) {
                            scores.add(score);
                        }
                    }
                    int highestScore = 0;

                    for (String possible : possibles) {
                        int index = possibles.indexOf(possible);
                        float lastHighest = scores.get(highestScore);
                        if (scores.get(index) > lastHighest) {
                            highestScore = index;
                        }
                    }

                    String value = possibles.get(highestScore);
                    Toast.makeText(getApplicationContext(), "You Said: " + value, Toast.LENGTH_LONG).show();
                    lastSpeechNode.update(value);
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                    log("Partial Results");
                }

                @Override
                public void onEvent(int eventType, Bundle params) {
                }
            });

            node.addChild(lastSpeechNode);
            node.addAction(startSpeechRecognitionAction);
            node.addAction(stopSpeechRecognitionAction);
        }
    }

    @TargetApi(20)
    private void setupHeartRateMonitor(DeviceNode node) {
        if (enableSensor(21)) {
            final DataValueNode rateNode = new DataValueNode("Heart_Rate", BasicMetaData.SIMPLE_INT);
            Sensor sensor = sensorManager.getDefaultSensor(21);

            sensorManager.registerListener(sensorEventListener(new SensorEventListener() {
                @Override
                public void onSensorChanged(@NonNull SensorEvent event) {
                    rateNode.update((double) event.values[0]);
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }
            }), sensor, SensorManager.SENSOR_DELAY_NORMAL);
            node.addChild(rateNode);
        }
    }

    @TargetApi(20)
    private void setupScreenProvider(DeviceNode node) {
        final DisplayManager displayManager = (DisplayManager) service.getSystemService(Context.DISPLAY_SERVICE);
        final DataValueNode screenOn = new DataValueNode("Screen_On", BasicMetaData.SIMPLE_BOOL);

        final DisplayManager.DisplayListener listener = new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayAdded(int i) {
            }

            @Override
            public void onDisplayRemoved(int i) {
            }

            @Override
            public void onDisplayChanged(int i) {
                Display display = displayManager.getDisplay(i);
                try {
                    Method method = display.getClass().getMethod("getState");
                    boolean on = (Boolean) method.invoke(display);
                    screenOn.update(on);
                } catch (NoSuchMethodException ignored) {
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        };

        displayManager.registerDisplayListener(listener, new Handler());

        onCleanup(new Action() {
            @Override
            public void run() {
                displayManager.unregisterDisplayListener(listener);
            }
        });

        node.addChild(screenOn);
    }

    public boolean enableSensor(int type) {
        return !sensorManager.getSensorList(type).isEmpty();
    }

    public int currentNotificationId = 0;

    public void startLink() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                log("Starting Link");

                linkStarted = true;

                link.addRootNode(rootNode);
                link.run(new String[0], false);
            }
        }).start();
    }

    public Context getApplicationContext() {
        return service.getApplicationContext();
    }

    public Handler handler;

    public void start() {
        googleClient.connect();
    }

    public void destroy() {
        for (Action action : cleanups) {
            action.run();
        }

        for (SensorEventListener eventListener : sensorListeners) {
            sensorManager.unregisterListener(eventListener);
        }

        if (recognizer != null) {
            recognizer.destroy();
        }
        timer.cancel();
        client.stop();
        googleClient.disconnect();
    }

    public SpeechRecognizer recognizer;

    public void log(String message) {
        Log.i(TAG, message);
    }
}
