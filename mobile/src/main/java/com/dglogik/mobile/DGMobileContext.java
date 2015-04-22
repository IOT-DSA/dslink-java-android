package com.dglogik.mobile;

import android.annotation.TargetApi;
import android.app.PendingIntent;
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
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Display;

import com.dglogik.mobile.ui.ControllerActivity;
import com.dglogik.mobile.wear.WearableSupport;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.Wearable;

import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkFactory;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.DSLinkProvider;
import org.dsa.iot.dslink.config.Configuration;
import org.dsa.iot.dslink.connection.ConnectionManager;
import org.dsa.iot.dslink.connection.ConnectionType;
import org.dsa.iot.dslink.handshake.LocalKeys;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.actions.Parameter;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.vertx.java.core.json.JsonObject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DGMobileContext {
    public static final String TAG = "DGMobile";
    public static DGMobileContext CONTEXT;
    public static boolean DEBUG = false;

    @NonNull
    public final LinkService service;
    @NonNull
    public final WearableSupport wearable;
    public final GoogleApiClient googleClient;
    @NonNull
    public DSLinkProvider link;
    public boolean linkStarted = false;

    @NonNull
    public final SensorManager sensorManager;
    @NonNull
    public final LocationManager locationManager;
    @NonNull
    public final PowerManager powerManager;

    public final SharedPreferences preferences;

    public Node currentDeviceNode;
    public Node devicesNode;
    public boolean mResolvingError;

    public DGMobileContext(@NonNull final LinkService service) {
        CONTEXT = this;
        this.service = service;
        this.handler = new Handler(getApplicationContext().getMainLooper());
        this.wearable = new WearableSupport(this);
        this.preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        DEBUG = Settings.Secure.getInt(getApplicationContext().getContentResolver(), Settings.Global.ADB_ENABLED, 0) == 1;
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
        powerManager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
    }

    private boolean stop = false;

    public void playSearchArtist(final String artist) {
        execute(new Executable() {
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
        execute(new Executable() {
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

    public void initialize() {
        System.setProperty("dslink.path", getApplicationContext().getFilesDir().getAbsolutePath());
        File fileDir = getApplicationContext().getFilesDir();
        if (!fileDir.exists()) {
            fileDir.mkdirs();
        }
        final String name = preferences.getString("link.name", "Android")
                .replaceAll("\\+", " ")
                .replaceAll(" ", "");
        final String brokerUrl = preferences.getString("broker.url", "");

        DSLinkHandler handler = new DSLinkHandler() {
            @Override
            public void preInit() {
                super.preInit();
                log("Pre-Init");
            }

            @Override
            public void onResponderInitialized(DSLink link) {
                super.onResponderInitialized(link);

                log("Initialized");

                devicesNode = link.getNodeManager().createRootNode("Devices").build();
                currentDeviceNode = devicesNode.createChild(Build.MODEL).build();
                setupCurrentDevice(currentDeviceNode);
            }

            @Override
            public void onResponderConnected(DSLink link) {
                super.onResponderConnected(link);

                log("Connected");
            }
        };

        LocalKeys keys;

        if (preferences.contains("link.key")) {
            keys = LocalKeys.deserialize(preferences.getString("link.key", ""));
        } else {
            keys = LocalKeys.generate();
            preferences.edit().putString("link.key", keys.serialize()).apply();
        }

        Configuration config = new Configuration();
        config.setConnectionType(ConnectionType.WEB_SOCKET);
        config.setDsId(name);
        config.setSerializationPath(fileDir);
        config.setResponder(true);
        config.setKeys(keys);
        config.setRequester(false);
        config.setAuthEndpoint(brokerUrl);
        config.validate();

        handler.setConfig(config);

        link = DSLinkFactory.generate(handler);

        startLink();

        if (preferences.getBoolean("feature.wear", false)) {
            wearable.initialize();
        }
    }

    public PackageManager getPackageManager() {
        return getApplicationContext().getPackageManager();
    }

    public void sendMusicCommand(final String command) {
        execute(new Executable() {
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

    public final List<Executable> cleanups = new ArrayList<>();
    public final List<SensorEventListener> sensorListeners = new ArrayList<>();

    public void onCleanup(Executable action) {
        cleanups.add(action);
    }

    public SensorEventListener sensorEventListener(SensorEventListener eventListener) {
        sensorListeners.add(eventListener);
        return eventListener;
    }

    public void setupCurrentDevice(@NonNull Node node)  {
        if (preferences.getBoolean("providers.location", false)) {
            final Node latitudeNode = node.createChild("Latitude").build();
            final Node longitudeNode = node.createChild("Longitude").build();

            latitudeNode.setConfig("type", new Value(ValueType.NUMBER.toJsonString()));
            longitudeNode.setConfig("type", new Value(ValueType.NUMBER.toJsonString()));

            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleClient);

            if (lastLocation != null) {
                latitudeNode.setValue(new Value(lastLocation.getLatitude()));
                longitudeNode.setValue(new Value(lastLocation.getLongitude()));
            } else {
                latitudeNode.setValue(new Value(0.0));
                longitudeNode.setValue(new Value(0.0));
            }

            LocationRequest request = new LocationRequest();

            request.setFastestInterval(500);
            request.setInterval(3000);
            request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            final LocationListener listener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    if (lastLatitude != location.getLatitude()) {
                        latitudeNode.setValue(new Value(location.getLatitude()));
                        lastLatitude = location.getLatitude();
                    }

                    if (lastLongitude != location.getLongitude()) {
                        longitudeNode.setValue(new Value(location.getLongitude()));
                        lastLongitude = location.getLatitude();
                    }
                }
            };

            LocationServices.FusedLocationApi.requestLocationUpdates(googleClient, request, listener);

            onCleanup(new Executable() {
                @Override
                public void run() {
                    LocationServices.FusedLocationApi.removeLocationUpdates(googleClient, listener);
                }
            });
        }

        if (enableNode("battery")) {
            final Node batteryLevelNode = node.createChild("Battery_Level").build();
            final Node chargerConnectedNode = node.createChild("Charger_Connected").build();
            final Node batteryFullNode = node.createChild("Battery_Full").build();

            batteryFullNode.setConfig("type", new Value(ValueType.NUMBER.toJsonString()));
            chargerConnectedNode.setConfig("type", new Value(ValueType.BOOL.toJsonString()));
            batteryFullNode.setConfig("type", new Value(ValueType.BOOL.toJsonString()));

            poller(new Executable() {
                @Override
                public void run() {
                    isBatteryLevelInitialized = true;

                    final Intent batteryStatus = getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                    assert batteryStatus != null;
                    int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                    int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

                    boolean isChargerConnected = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
                    boolean isFull = status == BatteryManager.BATTERY_STATUS_FULL;

                    double percent = (level / (float) scale) * 100;

                    if (lastBatteryLevel != percent) {
                        batteryLevelNode.setValue(new Value(percent));
                        lastBatteryLevel = percent;
                    }

                    if (lastChargerConnected != isChargerConnected) {
                        chargerConnectedNode.setValue(new Value(isChargerConnected));
                        lastChargerConnected = isChargerConnected;
                    }

                    if (lastBatteryFull != isFull) {
                        batteryFullNode.setValue(new Value(isFull));
                        lastBatteryFull = isFull;
                    }
                }
            }).poll(TimeUnit.SECONDS, 2, false);

            chargerConnectedNode.setValue(new Value(false));
            batteryFullNode.setValue(new Value(false));
        }

        if (Build.VERSION.SDK_INT >= 20 && preferences.getBoolean("providers.screen", false)) {
            setupScreenProvider(node);
        }

        if (enableNode("activity")) {
            activityNode = node.createChild("Activity").build();

            activityNode.setConfig("type", new Value(ValueType.STRING.toJsonString()));

            final PendingIntent intent = PendingIntent.getService(getApplicationContext(), 40, new Intent(getApplicationContext(), ActivityRecognitionIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(googleClient, 1000, intent);

            onCleanup(new Executable() {
                @Override
                public void run() {
                    ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(googleClient, intent);
                }
            });
        }

        if (enableSensor("steps", 19)) {
            final Node stepsNode = node.createChild("Steps").build();
            stepsNode.setConfig("type", new Value(ValueType.NUMBER.toJsonString()));

            Sensor sensor = sensorManager.getDefaultSensor(19);

            sensorManager.registerListener(sensorEventListener(new SensorEventListener() {
                @Override
                public void onSensorChanged(@NonNull SensorEvent event) {
                    stepsNode.setValue(new Value(event.values[0]));
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }
            }), sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        if (enableSensor("heart_rate", 21)) {
            setupHeartRateMonitor(node);
        }

        if (enableSensor("temperature", Sensor.TYPE_AMBIENT_TEMPERATURE)) {
            final Node tempCNode = node.createChild("Ambient_Temperature_Celsius").build();
            final Node tempFNode = node.createChild("Ambient_Temperature_Fahrenheit").build();

            Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);

            sensorManager.registerListener(sensorEventListener(new SensorEventListener() {
                @Override
                public void onSensorChanged(@NonNull SensorEvent event) {
                    double celsius = (double) event.values[0];
                    double fahrenheit = 32 + (celsius * 9 / 5);
                    tempCNode.setValue(new Value(celsius));
                    tempFNode.setValue(new Value(fahrenheit));
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }
            }), sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        if (enableSensor("light_level", Sensor.TYPE_LIGHT)) {
            final Node lux = node.createChild("Light_Level").build();

            Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

            sensorManager.registerListener(sensorEventListener(new SensorEventListener() {
                @Override
                public void onSensorChanged(@NonNull SensorEvent event) {
                    lux.setValue(new Value(event.values[0]));
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }
            }), sensor, 6000);
        }

        if (enableSensor("pressure", Sensor.TYPE_PRESSURE)) {
            final Node pressure = node.createChild("Air_Pressure").build();

            pressure.setConfig("type", new Value(ValueType.NUMBER.toJsonString()));

            Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);

            sensorManager.registerListener(sensorEventListener(new SensorEventListener() {
                @Override
                public void onSensorChanged(@NonNull SensorEvent event) {
                    pressure.setValue(new Value(event.values[0]));
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }
            }), sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        if (enableSensor("humidity", Sensor.TYPE_RELATIVE_HUMIDITY)) {
            final Node humidity = node.createChild("Humidity").build();

            humidity.setConfig("type", new Value(ValueType.NUMBER.toJsonString()));

            Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);

            sensorManager.registerListener(sensorEventListener(new SensorEventListener() {
                @Override
                public void onSensorChanged(@NonNull SensorEvent event) {
                    humidity.setValue(new Value(event.values[0]));
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }
            }), sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        if (enableSensor("proximity", Sensor.TYPE_PROXIMITY)) {
            final Node proximity = node.createChild("Proximity").build();

            proximity.setConfig("type", new Value(ValueType.NUMBER.toJsonString()));

            Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

            sensorManager.registerListener(sensorEventListener(new SensorEventListener() {
                @Override
                public void onSensorChanged(@NonNull SensorEvent event) {
                    proximity.setValue(new Value(event.values[0]));
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }
            }), sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        if (enableSensor("gyroscope", Sensor.TYPE_GYROSCOPE)) {
            final Node x = node.createChild("Gyroscope_X").build();
            final Node y = node.createChild("Gyroscope_Y").build();
            final Node z = node.createChild("Gyroscope_Z").build();

            for (Node m : new Node[]{x, y, z}) {
                m.setConfig("type", new Value(ValueType.NUMBER.toJsonString()));
            }

            Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

            sensorManager.registerListener(sensorEventListener(new SensorEventListener() {
                @Override
                public void onSensorChanged(@NonNull SensorEvent event) {
                    x.setValue(new Value(event.values[0]));
                    y.setValue(new Value(event.values[1]));
                    z.setValue(new Value(event.values[2]));
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

            Node speakNode = currentDeviceNode.createChild("Speak").setAction(new Action(Permission.WRITE, new org.vertx.java.core.Handler<ActionResult>() {
                @Override
                public void handle(ActionResult event) {
                    speech.speak(event.getJsonIn().getObject("params").getString("text"), TextToSpeech.QUEUE_ADD, new HashMap<String, String>());
                }
            }).addParameter(new Parameter("text", ValueType.STRING, new Value("")))).build();

            onCleanup(new Executable() {
                @Override
                public void run() {
                    speech.stop();
                }
            });
        }

        if (preferences.getBoolean("actions.show_maps", true)) {
            Node a = node.createChild("ShowLocationMap").setAction(new Action(Permission.WRITE, new org.vertx.java.core.Handler<ActionResult>() {
                @Override
                public void handle(ActionResult event) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    JsonObject params = event.getJsonIn().getObject("params");
                    intent.setData(Uri.parse("geo:" + params.getString("latitude") + "," + params.getString("longitude")));
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }
                }
            }).addParameter(new Parameter("latitude", ValueType.NUMBER, new Value(0.0))).addParameter(new Parameter("longitude", ValueType.NUMBER, new Value(0.0)))).build();

            Node b = node.createChild("ShowMap").setAction(new Action(Permission.WRITE, new org.vertx.java.core.Handler<ActionResult>() {
                @Override
                public void handle(ActionResult event) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);

                    try {
                        intent.setData(Uri.parse("geo:0,0?q=" + URLEncoder.encode(event.getJsonIn().getObject("params").getString("query"), "UTF-8")));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }
                }
            })).build();
        }

        if (preferences.getBoolean("actions.open_url", true)) {
            final Action openUrlAction = new Action(Permission.WRITE, new org.vertx.java.core.Handler<ActionResult>() {

                @Override
                public void handle(ActionResult event) {
                    JsonObject params = event.getJsonIn().getObject("params");
                    final Uri url = Uri.parse(params.getString("url"));
                    execute(new Executable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(Intent.ACTION_VIEW, url);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            if (intent.resolveActivity(getPackageManager()) != null) {
                                getApplicationContext().startActivity(intent);
                            }
                        }
                    });
                }
            });

            openUrlAction.addParameter(new Parameter("url", ValueType.STRING, new Value("http://wwww.google.com")));

            node.createChild("OpenUrl").setAction(openUrlAction).build();
        }

        if (preferences.getBoolean("actions.search", true)) {
            final Action searchAction = new Action(Permission.WRITE, new org.vertx.java.core.Handler<ActionResult>() {
                @Override
                public void handle(ActionResult event) {
                    final String query = event.getJsonIn().getObject("params").getString("query");
                    execute(new Executable() {
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
                }
            });
            node.createChild("Search").setAction(searchAction).build();
        }
        if (preferences.getBoolean("providers.speech", true)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());

            final Node lastSpeechNode = node.createChild("Recognized_Speech").build();

            final Action startSpeechRecognitionAction = new Action(Permission.WRITE, new org.vertx.java.core.Handler<ActionResult>() {
                @Override
                public void handle(ActionResult event) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent();
                            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                            recognizer.startListening(intent);
                        }
                    });
                }
            });


            final Action stopSpeechRecognitionAction = new Action(Permission.WRITE, new org.vertx.java.core.Handler<ActionResult>() {
                @Override
                public void handle(ActionResult event) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            recognizer.stopListening();
                        }
                    });
                }
            });

            Node startSpeechRecognition = node.createChild("StartSpeechRecognition").build();
            startSpeechRecognition.setAction(startSpeechRecognitionAction);
            Node stopSpeechRecognition = node.createChild("StopSpeechRecognition").build();
            stopSpeechRecognition.setAction(stopSpeechRecognitionAction);

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
                    lastSpeechNode.setValue(new Value(value));
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                    log("Partial Results");
                }

                @Override
                public void onEvent(int eventType, Bundle params) {
                }
            });
        }
    }

    private boolean isBatteryLevelInitialized = false;

    @TargetApi(20)
    private void setupHeartRateMonitor(Node node) {
        Sensor sensor = sensorManager.getDefaultSensor(21);

        if (sensor == null) return;

        final Node rateNode = node.createChild("Heart_Rate").build();

        node.setConfig("type", new Value(ValueType.NUMBER.toJsonString()));

        sensorManager.registerListener(sensorEventListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(@NonNull SensorEvent event) {
                rateNode.setValue(new Value(event.values[0]));
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        }), sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected Node activityNode;

    @TargetApi(20)
    private void setupScreenProvider(Node node) {
        final DisplayManager displayManager = (DisplayManager) service.getSystemService(Context.DISPLAY_SERVICE);
        final Node screenOn = node.createChild("Screen_On").build();

        screenOn.setConfig("type", new Value(ValueType.BOOL.toJsonString()));

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
                    screenOn.setValue(new Value(on));
                } catch (NoSuchMethodException ignored) {
                } catch (InvocationTargetException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        };

        displayManager.registerDisplayListener(listener, new Handler());

        onCleanup(new Executable() {
            @Override
            public void run() {
                displayManager.unregisterDisplayListener(listener);
            }
        });

        screenOn.setValue(new Value(powerManager.isScreenOn()));
    }

    public boolean enableSensor(String id, int type) {
        return enableNode(id) && !sensorManager.getSensorList(type).isEmpty();
    }

    public int currentNotificationId = 0;

    public void startLink() {
        link.start();
    }

    public void execute(Executable action) {
        handler.post(action);
    }

    public Context getApplicationContext() {
        return service.getApplicationContext();
    }

    public final Handler handler;
    private double lastBatteryLevel = 0.0;
    private boolean lastBatteryFull = false;

    public void start() {
        googleClient.connect();
    }

    public void destroy() {
        log("Running Destruction Actions");
        for (Executable action : cleanups) {
            action.run();
        }

        log("Un-registering Sensor Event Listeners");
        for (SensorEventListener eventListener : sensorListeners) {
            sensorManager.unregisterListener(eventListener);
        }

        if (recognizer != null) {
            log("Destroying Speech Recognizer");
            try {
                recognizer.destroy();
            } catch (Exception ignored) {}
        }

        link.stop();

        log("Link Stopped");
        log("Clearing Device Nodes");

        log("Disconnecting Google API Client");
        googleClient.disconnect();
    }

    private boolean lastChargerConnected = false;

    public SpeechRecognizer recognizer;

    public Poller poller(Executable action) {
        final Poller poller = new Poller(action);
        onCleanup(new Executable() {
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
