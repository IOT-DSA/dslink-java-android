package com.dglogik.mobile;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Display;
import android.widget.Toast;

import com.dglogik.api.BasicMetaData;
import com.dglogik.api.DGMetaData;
import com.dglogik.api.server.AbstractTunnelClient;
import com.dglogik.dslink.Application;
import com.dglogik.dslink.client.Client;
import com.dglogik.dslink.client.command.base.ArgValue;
import com.dglogik.dslink.client.command.base.ArgValueMetadata;
import com.dglogik.dslink.client.command.base.Options;
import com.dglogik.dslink.node.Poller;
import com.dglogik.dslink.node.base.BaseAction;
import com.dglogik.dslink.node.base.BaseNode;
import com.dglogik.dslink.tunnel.TunnelClientFactory;
import com.dglogik.mobile.link.DataValueNode;
import com.dglogik.mobile.link.DeviceNode;
import com.dglogik.mobile.link.MusicNode;
import com.dglogik.mobile.link.RootNode;
import com.dglogik.mobile.link.AudioSystemNode;
import com.dglogik.mobile.ui.ControllerActivity;
import com.dglogik.mobile.wear.WearableSupport;
import com.dglogik.table.Table;
import com.dglogik.table.Tables;
import com.dglogik.value.DGValue;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.location.*;
import android.app.*;
import com.google.android.gms.wearable.Wearable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import ext.javax.servlet.ServletContext;

public class DGMobileContext {
    public static final String TAG = "DGMobile";
    public static DGMobileContext CONTEXT;
    public AndroidTunnelClient tunnelClient;

    @NonNull
    public final LinkService service;
    @NonNull
    public final WearableSupport wearable;
    public final GoogleApiClient googleClient;
    @NonNull
    public final Application link;
    public boolean linkStarted = false;

    @NonNull
    public final SensorManager sensorManager;
    @NonNull
    public final LocationManager locationManager;
    @NonNull
    public final PowerManager powerManager;
    @NonNull
    public final Client client;
    public final SharedPreferences preferences;

    public static final RootNode<DeviceNode> devicesNode = new RootNode<>("Devices");

    public DeviceNode currentDeviceNode;
    public FitnessSupport fitness;
    public boolean mResolvingError;

    public DGMobileContext(@NonNull final LinkService service) {
        CONTEXT = this;
        this.service = service;
        this.handler = new Handler(getApplicationContext().getMainLooper());
        this.wearable = new WearableSupport(this);
        this.fitness = new FitnessSupport(this);
        this.preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        GoogleApiClient.Builder apiClientBuilder = new GoogleApiClient.Builder(getApplicationContext())
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.i(TAG, "Google API Client Connected");

                        initialize();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        if (connectionResult.getErrorCode() == FitnessStatusCodes.NEEDS_OAUTH_PERMISSIONS) {
                            try {
                                connectionResult.startResolutionForResult(
                                        ControllerActivity.INSTANCE,
                                        50);
                            } catch (IntentSender.SendIntentException e) {
                                e.printStackTrace();
                            }
                            return;
                        }
                        Log.e(TAG, "Google API Client Connection Failed! Code = " + connectionResult.getErrorCode());
                        ControllerActivity.DID_FAIL = true;
                        ControllerActivity.ERROR_MESSAGE = "Google API Client Failed to Connect: Code = " + connectionResult.getErrorCode();
                    }
                });

        apiClientBuilder.addApi(LocationServices.API);
	apiClientBuilder.addApi(ActivityRecognition.API);

        apiClientBuilder.setHandler(handler);

        if (preferences.getBoolean("feature.wear", false)) {
            apiClientBuilder.addApi(Wearable.API);
        }

        if (preferences.getBoolean("feature.fitness", false)) {
            apiClientBuilder.addApi(Fitness.API);
            apiClientBuilder
                    .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ))
                    .addScope(new Scope(Scopes.FITNESS_BODY_READ));
            apiClientBuilder.setAccountName(preferences.getString("account.name", null));
        }

        this.googleClient = apiClientBuilder.build();

        this.sensorManager = (SensorManager) service.getSystemService(LinkService.SENSOR_SERVICE);
        this.locationManager = (LocationManager) service.getSystemService(LinkService.LOCATION_SERVICE);
        this.link = Application.get();

        this.client = new Client(false) {
            @Override
            public void run() {
                stop = false;
                try { Thread.sleep(2000); } catch (Exception ignored) {}
                log("Running Client");
                while(!stop) { try { Thread.sleep(100); } catch (Exception ignored) {} }
                log("Client Complete");
            }

            @Override
            protected void onStop() {
                stop = true;
            }
        };

        link.setClient(client);

        link.setTunnelClientFactory(new TunnelClientFactory() {
            @Override
            public AbstractTunnelClient create(ServletContext servletContext, String uri) {
                tunnelClient = new AndroidTunnelClient(servletContext, uri);

                return tunnelClient;
            }
        });
        powerManager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
    }

    private boolean stop = false;

    public void playSearchArtist(final String artist) {
        execute(new Action() {
            @Override
            public void run() {
                Intent intent = new Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH);
                intent.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE);
                intent.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, artist);
                intent.putExtra(SearchManager.QUERY, artist);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        });
    }

    public void playSearchSong(final String song) {
        execute(new Action() {
            @Override
            public void run() {
                Intent intent = new Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH);
                intent.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE);
                intent.putExtra(MediaStore.EXTRA_MEDIA_TITLE, song);
                intent.putExtra(SearchManager.QUERY, song);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        });
    }

    public static boolean addedRoot = false;

    public void initialize() {
        if (preferences.getBoolean("feature.wear", false)) {
            wearable.initialize();
        }

        if (preferences.getBoolean("feature.fitness", false)) {
            fitness.initialize();
        }

        if (!addedRoot) {
            link.addRootNode(devicesNode);
            addedRoot = true;
        }

        currentDeviceNode = new DeviceNode(Build.MODEL);
        setupCurrentDevice(currentDeviceNode);

        devicesNode.addChild(currentDeviceNode);

        startLink();
    }

    public PackageManager getPackageManager() {
        return getApplicationContext().getPackageManager();
    }

    public void sendMusicCommand(final String command) {
        execute(new Action() {
            @Override
            public void run() {
                Intent intent = new Intent("com.android.music.musicservicecommand");
                intent.putExtra("command", command);
                getApplicationContext().sendBroadcast(intent);
            }
        });
    }

    public boolean enableNode(String id) {
        NodeDescriptor desc = null;
        for (NodeDescriptor descriptor : DGConstants.NODES) {
            if (descriptor.getId().equals(id)) {
                desc = descriptor;
            }
        }

        if (desc == null) {
            log("No Descriptor found for node: " + id);
            log("Defaulting to Disabled");
            return false;
        }

        return preferences.getBoolean("providers." + id, desc.isDefaultEnabled());
    }

    public void startActivity(Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getApplicationContext().startActivity(intent);
    }

    public double lastLatitude;
    public double lastLongitude;

    public final List<Action> cleanups = new ArrayList<>();
    public final List<SensorEventListener> sensorListeners = new ArrayList<>();

    public void onCleanup(Action action) {
        cleanups.add(action);
    }

    public SensorEventListener sensorEventListener(SensorEventListener eventListener) {
        sensorListeners.add(eventListener);
        return eventListener;
    }

    public void setupCurrentDevice(@NonNull DeviceNode node) {
        if (preferences.getBoolean("providers.location", false)) {
            final DataValueNode latitudeNode = new DataValueNode("Location_Latitude", BasicMetaData.SIMPLE_INT);
            final DataValueNode longitudeNode = new DataValueNode("Location_Longitude", BasicMetaData.SIMPLE_INT);

            latitudeNode.initializeValue = new Action () {
                @Override
                public void run() {
                    latitudeNode.update(LocationServices.FusedLocationApi.getLastLocation(googleClient).getLatitude());
                }
            };

            longitudeNode.initializeValue = new Action () {
                @Override
                public void run() {
                    longitudeNode.update(LocationServices.FusedLocationApi.getLastLocation(googleClient).getLongitude());
                }
            };

            LocationRequest request = new LocationRequest();

            request.setFastestInterval(500);
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

        if (preferences.getBoolean("providers.battery", false)) {
            final DataValueNode batteryLevelNode = new DataValueNode("Battery_Level", BasicMetaData.SIMPLE_INT);
            final DataValueNode chargerConnectedNode = new DataValueNode("Charger_Connected", BasicMetaData.SIMPLE_BOOL);
            final DataValueNode batteryFullNode = new DataValueNode("Battery_Full", BasicMetaData.SIMPLE_BOOL);

                poller(new Action() {
                    @Override
                    public void run() {
                        final Intent batteryStatus = getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                        assert batteryStatus != null;
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
                }).poll(TimeUnit.SECONDS, 4, false);

                node.addChild(batteryLevelNode);
                node.addChild(batteryFullNode);
                node.addChild(chargerConnectedNode);

        }

        if (Build.VERSION.SDK_INT >= 20 && preferences.getBoolean("providers.screen", false)) {
            setupScreenProvider(node);
        }

        if (enableNode("activity")) {
            activityNode = new DataValueNode("Activity", BasicMetaData.SIMPLE_STRING);

            final PendingIntent intent = PendingIntent.getService(getApplicationContext(), 40, new Intent(getApplicationContext(), ActivityRecognitionIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(googleClient, 1000, intent);

            onCleanup(new Action() {
                @Override
                public void run() {
                    ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(googleClient, intent);
                }
            });

            node.addChild(activityNode);
       }

        if (preferences.getBoolean("actions.notifications", true)) {
            final NotificationManager notificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);

            final BaseAction createNotificationAction = new BaseAction("CreateNotification") {
                @NonNull
                @Override
                public Table invoke(BaseNode baseNode, @NonNull Map<String, DGValue> args) {
                    Notification.Builder builder = new Notification.Builder(getApplicationContext());

                    builder.setContentTitle(args.get("title").toString());
                    builder.setContentText(args.get("content").toString());
                    builder.setSmallIcon(R.drawable.ic_launcher);

                    Notification notification = builder.build();

                    currentNotificationId++;

                    notificationManager.notify(currentNotificationId, notification);

                    return Tables.makeTable(new HashMap<String, DGMetaData>() {{
                        put("id", BasicMetaData.SIMPLE_INT);
                    }}, new HashMap<String, DGValue>() {{
                        put("id", DGValue.make(currentNotificationId));
                    }});
                }
            };

            createNotificationAction.addParam("title", BasicMetaData.SIMPLE_STRING);
            createNotificationAction.addParam("content", BasicMetaData.SIMPLE_STRING);

            final BaseAction destroyNotificationAction = new BaseAction("DestroyNotification") {
                @Override
                public Table invoke(BaseNode baseNode, @NonNull Map<String, DGValue> args) {
                    int id = args.get("id").toInt();

                    notificationManager.cancel(id);
                    return null;
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

        if (enableSensor("steps", 19)) {
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

//        if (enableSensor("heart_rate", 21)) {
//            setupHeartRateMonitor(node);
//        }

        if (enableSensor("temperature", Sensor.TYPE_AMBIENT_TEMPERATURE)) {
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

        if (enableSensor("light_level", Sensor.TYPE_LIGHT)) {
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

        if (enableSensor("pressure", Sensor.TYPE_PRESSURE)) {
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

        if (enableSensor("humidity", Sensor.TYPE_RELATIVE_HUMIDITY)) {
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

        if (enableSensor("proximity", Sensor.TYPE_PROXIMITY)) {
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

        if (enableSensor("gyroscope", Sensor.TYPE_GYROSCOPE)) {
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

            currentDeviceNode.addChild(x);
            currentDeviceNode.addChild(y);
            currentDeviceNode.addChild(z);
        }

        if (preferences.getBoolean("actions.speak", true)) {
            final TextToSpeech speech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                }
            });

            final BaseAction speakAction = new BaseAction("Speak") {
                @SuppressWarnings("deprecation")
                @Override
                public Table invoke(BaseNode baseNode, @NonNull Map<String, DGValue> args) {
                    speech.speak(args.get("text").toString(), TextToSpeech.QUEUE_ADD, new HashMap<String, String>());
                    return null;
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

        if (preferences.getBoolean("actions.open_url", true)) {
            final BaseAction openUrlAction = new BaseAction("OpenUrl") {
                @Override
                public Table invoke(BaseNode baseNode, @NonNull Map<String, DGValue> args) {
                    final Uri url = Uri.parse(args.get("url").toString());
                    execute(new Action() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(Intent.ACTION_VIEW, url);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            if (intent.resolveActivity(getPackageManager()) != null) {
                                getApplicationContext().startActivity(intent);
                            }
                        }
                    });
                    return null;
                }
            };
            openUrlAction.addParam("url", BasicMetaData.SIMPLE_STRING);

            node.addAction(openUrlAction);
        }

        if (enableNode("audio")) {
            final AudioSystemNode audioSystemNode = new AudioSystemNode();
            final AudioManager manager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

            audioSystemNode.addVolumeStream(manager, "Notification", AudioManager.STREAM_NOTIFICATION);
            audioSystemNode.addVolumeStream(manager, "System", AudioManager.STREAM_SYSTEM);
            audioSystemNode.addVolumeStream(manager, "Music", AudioManager.STREAM_MUSIC);
            audioSystemNode.addVolumeStream(manager, "Call", AudioManager.STREAM_VOICE_CALL);
            audioSystemNode.addVolumeStream(manager, "Ringer", AudioManager.STREAM_RING);
            audioSystemNode.addVolumeStream(manager, "Alarm", AudioManager.STREAM_ALARM);

            audioSystemNode.setupInformationNodes(manager);

            node.addChild(audioSystemNode);
        }

        if (preferences.getBoolean("actions.search", true)) {
            final BaseAction searchWebAction = new BaseAction("SearchWeb") {
                @Override
                public Table invoke(BaseNode baseNode, @NonNull Map<String, DGValue> args) {
                    final String query = args.get("query").toString();
                    execute(new Action() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(Intent.ACTION_SEARCH);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.putExtra(SearchManager.QUERY, query);
                            if (intent.resolveActivity(service.getPackageManager()) != null) {
                                service.startActivity(intent);
                            }
                        }
                    });
                    return null;
                }
            };
            searchWebAction.addParam("query", BasicMetaData.SIMPLE_STRING);

            node.addAction(searchWebAction);
        }

        if (enableNode("music")) {
            MusicNode musicNode = new MusicNode();
            musicNode.init();
            node.addChild(musicNode);
        }

        if (preferences.getBoolean("providers.speech", true)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());

            final DataValueNode lastSpeechNode = new DataValueNode("Recognized_Speech", BasicMetaData.SIMPLE_STRING);

            final BaseAction startSpeechRecognitionAction = new BaseAction("StartSpeechRecognition") {
                @SuppressWarnings("deprecation")
                @Override
                public Table invoke(BaseNode baseNode, @NonNull Map<String, DGValue> args) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            recognizer.startListening(new Intent());
                        }
                    });
                    return null;
                }
            };

            final BaseAction stopSpeechRecognitionAction = new BaseAction("StopSpeechRecognition") {
                @SuppressWarnings("deprecation")
                @Override
                public Table invoke(BaseNode baseNode, @NonNull Map<String, DGValue> args) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            recognizer.stopListening();
                        }
                    });
                    return null;
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
                    List<Float> scores = new ArrayList<>();
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

        if (enableNode("power")) {
            log("Power Management Features Enabled");
            setupPowerProvider(node);
        }
    }

//    @TargetApi(20)
//    private void setupHeartRateMonitor(DeviceNode node) {
//        final DataValueNode rateNode = new DataValueNode("Heart_Rate", BasicMetaData.SIMPLE_INT);
//        Sensor sensor = sensorManager.getDefaultSensor(21);
//
//        sensorManager.registerListener(sensorEventListener(new SensorEventListener() {
//            @Override
//            public void onSensorChanged(@NonNull SensorEvent event) {
//                rateNode.update((double) event.values[0]);
//            }
//
//            @Override
//            public void onAccuracyChanged(Sensor sensor, int accuracy) {
//            }
//        }), sensor, SensorManager.SENSOR_DELAY_NORMAL);
//        node.addChild(rateNode);
//    }

    protected DataValueNode activityNode;

    private String getActivityNameFromType(int activityType) {
        switch(activityType) {
            case DetectedActivity.IN_VEHICLE:
                return "in_vehicle";
            case DetectedActivity.ON_BICYCLE:
                return "on_bicycle";
            case DetectedActivity.ON_FOOT:
                return "on_foot";
            case DetectedActivity.STILL:
                return "still";
            case DetectedActivity.UNKNOWN:
                return "unknown";
            case DetectedActivity.TILTING:
                return "tilting";
        }
        return "unknown";
    }

    private void setupPowerProvider(DeviceNode node) {
        BaseAction wakeUpAction = new BaseAction("WakeUp") {
            @Override
            public Table invoke(BaseNode baseNode, @NonNull Map<String, DGValue> args) {
                long time = args.get("time").toLong();
                powerManager.wakeUp(time);
                return null;
            }
        };

        BaseAction sleepAction = new BaseAction("Sleep") {
            @Override
            public Table invoke(BaseNode baseNode, @NonNull Map<String, DGValue> args) {
                long time = args.get("time").toLong();
                powerManager.goToSleep(time);
                return null;
            }
        };

        wakeUpAction.addParam("time", BasicMetaData.SIMPLE_INT);
        sleepAction.addParam("time", BasicMetaData.SIMPLE_INT);

        node.addAction(wakeUpAction);
        node.addAction(sleepAction);
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
                    boolean on = ((Integer) method.invoke(display)) == 2;
                    screenOn.update(on);
                } catch (NoSuchMethodException ignored) {
                } catch (InvocationTargetException | IllegalAccessException e) {
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

        screenOn.update(powerManager.isScreenOn());

        node.addChild(screenOn);
    }

    public boolean enableSensor(String id, int type) {
        return enableNode(id) && !sensorManager.getSensorList(type).isEmpty();
    }

    public int currentNotificationId = 0;

    @SuppressWarnings("FieldCanBeLocal")
    private Thread linkThread;

    public void startLink() {
        linkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                log("Starting Link");

                linkStarted = true;

                final String name = preferences.getString("link.name", "Android")
                        .replaceAll("\\+", " ")
                        .replaceAll(" ", "");
                final String brokerUrl = preferences.getString("broker.url", "");

                link.run(new String[0], false, new Options(new HashMap<String, ArgValue>() {{
                    put("url", new ArgValue(new ArgValueMetadata().setType(ArgValueMetadata.Type.STRING)).set(brokerUrl));
                    put("name", new ArgValue(new ArgValueMetadata().setType(ArgValueMetadata.Type.STRING)).set(name));
                }}, false));

                log("Link Stopped");

                linkStarted = false;
            }
        });
        linkThread.start();
    }

    public void execute(Action action) {
        handler.post(action);
    }

    public Context getApplicationContext() {
        return service.getApplicationContext();
    }

    public final Handler handler;

    public void start() {
        googleClient.connect();
    }

    public void destroy() {
        log("Running Destruction Actions");
        for (Action action : cleanups) {
            action.run();
        }

        log("Un-registering Sensor Event Listeners");
        for (SensorEventListener eventListener : sensorListeners) {
            sensorManager.unregisterListener(eventListener);
        }

        if (recognizer != null) {
            log("Destroying Speech Recognizer");
            recognizer.destroy();
        }

        if (linkStarted) {
            log("Stopping Link");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    link.stop();
                    log("Link Stopped");
                    log("Clearing Device Nodes");
                    devicesNode.clearChildren();
                }
            }).start();
        }

        log("Disconnecting Google API Client");
        googleClient.disconnect();
    }

    public SpeechRecognizer recognizer;

    public Poller poller(Action action) {
        final Poller poller = new Poller(action);
        onCleanup(new Action() {
            @Override
            public void run() {
                if (poller.running()) {
                    poller.cancel();
                }
            }
        });

        return poller;
    }

    public static void log(String message) {
        Log.i(TAG, message);
    }
}
