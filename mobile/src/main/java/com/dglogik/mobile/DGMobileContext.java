package com.dglogik.mobile;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
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
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.view.Display;
import android.widget.Toast;

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

import org.acra.ACRA;
import org.acra.ErrorReporter;
import org.acra.ExceptionHandlerInitializer;
import org.dsa.iot.dslink.DSLink;
import org.dsa.iot.dslink.DSLinkFactory;
import org.dsa.iot.dslink.DSLinkHandler;
import org.dsa.iot.dslink.DSLinkProvider;
import org.dsa.iot.dslink.config.Configuration;
import org.dsa.iot.dslink.connection.ConnectionType;
import org.dsa.iot.dslink.handshake.LocalKeys;
import org.dsa.iot.dslink.methods.StreamState;
import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.actions.Parameter;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.dsa.iot.dslink.serializer.Serializer;
import org.dsa.iot.dslink.util.Objects;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("deprecation")
public class DGMobileContext {
    public static final String TAG = "DGMobile";
    public static DGMobileContext CONTEXT;
    public static boolean DEBUG = false;

    @NonNull
    public final LinkService service;
    @NonNull
    public final WearableSupport wearable;
    public final GoogleApiClient googleClient;
    public DSLinkProvider link;

    @NonNull
    public final SensorManager sensorManager;
    @NonNull
    public final LocationManager locationManager;
    @NonNull
    public final PowerManager powerManager;

    public final SharedPreferences preferences;

    public Node currentDeviceNode;
    public Node devicesNode;

    public DGMobileContext(@NonNull final LinkService service) {
        ACRA.getErrorReporter().setExceptionHandlerInitializer(new ExceptionHandlerInitializer() {
            @Override
            public void initializeExceptionHandler(ErrorReporter reporter) {
                StringBuilder enabledBuilder = new StringBuilder();
                StringBuilder disabledBuilder = new StringBuilder();
                for (NodeDescriptor descriptor : DGConstants.NODES) {
                    boolean enabled = preferences.getBoolean("providers." + descriptor.getId(), descriptor.isDefaultEnabled());
                    if (enabled) {
                        if (enabledBuilder.length() != 0) {
                            enabledBuilder.append(",");
                        }
                        enabledBuilder.append(descriptor.getName());
                    } else {
                        if (disabledBuilder.length() != 0) {
                            disabledBuilder.append(",");
                        }
                        disabledBuilder.append(descriptor.getName());
                    }
                }

                reporter.putCustomData("enabledNodes", enabledBuilder.toString());
                reporter.putCustomData("disabledNodes", disabledBuilder.toString());
                reporter.putCustomData("wearEnabled", "" + preferences.getBoolean("feature.wear", false));
                reporter.putCustomData("fitnessEnabled", "" + preferences.getBoolean("feature.fitness", false));
                if (realLink != null) {
                    try {
                        Serializer serializer = new Serializer(realLink.getNodeManager());
                        reporter.putCustomData("nodes", serializer.serialize().encode());
                    } catch (Exception ignored) {
                    }
                }
            }
        });

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
            apiClientBuilder.addApi(Fitness.SENSORS_API);
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

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void initialize() {
        for (String key : new String[] {
                "brokerUrl",
                "linkName",
                "responderConnected",
                "responderInitialized"
        }) {
            ACRA.getErrorReporter().removeCustomData(key);
        }

        Objects.setThreadPool(Poller.STPE);
        Objects.setDaemonThreadPool(Poller.STPE);
        System.setProperty("dslink.path", getApplicationContext().getFilesDir().getAbsolutePath());
        File fileDir = getApplicationContext().getFilesDir();
        if (!fileDir.exists()) {
            fileDir.mkdirs();
        }
        final String name = preferences.getString("link.name", "Android")
                .replaceAll("\\+", " ")
                .replaceAll(" ", "");
        final String brokerUrl = preferences.getString("broker.url", "");

        ACRA.getErrorReporter().putCustomData("linkName", name);
        ACRA.getErrorReporter().putCustomData("brokerUrl", brokerUrl);
        final DSLinkHandler handler = new DSLinkHandler() {
            @Override
            public void preInit() {
                super.preInit();
                log("Pre-Init");
            }

            @Override
            public void onResponderInitialized(DSLink link) {
                super.onResponderInitialized(link);

                realLink = link;

                ACRA.getErrorReporter().putCustomData("responderInitialized", "true");

                log("Initialized");
                execute(new Executable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "DSAndroid Initialized", Toast.LENGTH_SHORT).show();
                    }
                });

                devicesNode = link.getNodeManager().createRootNode("Devices").build();
                String DEVICE_ID = Build.SERIAL != null ? Build.SERIAL : Build.MODEL;

                if (DEVICE_ID == null) {
                    DEVICE_ID = Build.DEVICE;
                }

                currentDeviceNode = devicesNode.createChild(DEVICE_ID).build();
                currentDeviceNode.setDisplayName(Build.MODEL);
                execute(new Executable() {
                    @Override
                    public void run() {
                        setupCurrentDevice(currentDeviceNode);
                    }
                });
            }

            @Override
            public void onResponderConnected(DSLink link) {
                super.onResponderConnected(link);
                execute(new Executable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "DSAndroid Connected", Toast.LENGTH_SHORT).show();
                    }
                });
                ACRA.getErrorReporter().putCustomData("responderConnected", "true");
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

        File file = new File(fileDir.getAbsolutePath() + "/" + "dslink.json");

        // We have to delete this. It's a sad fact.
        if (file.exists()) {
            file.delete();
        }

        Configuration config = new Configuration();
        config.setConnectionType(ConnectionType.WEB_SOCKET);
        config.setDsId(name);
        config.setSerializationPath(file);
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

    DSLink realLink;

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
        setupOpenApplicationProvider(node);

        if (preferences.getBoolean("providers.location", false)) {
            final Node latitudeNode = node.createChild("Latitude").setValueType(ValueType.NUMBER).build();
            final Node longitudeNode = node.createChild("Longitude").setValueType(ValueType.NUMBER).build();

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

        if (enableNode("music")) {
            final Node sendMusicCommandNode = node.createChild("Send_Music_Command")
                    .setAction(new Action(Permission.WRITE, new org.vertx.java.core.Handler<ActionResult>() {
                        @Override
                        public void handle(ActionResult e) {
                            String command = e.getParameter("command").getString();
                            sendMusicCommand(command);
                        }
                    })
                            .addParameter(new Parameter("command",
                                    ValueType.makeEnum("play", "pause", "stop", "next", "previous", "togglepause")
                            ))).build();
        }

        if (enableNode("current_app")) {
            final Node getForegroundApplicationNode = node.createChild("Get_Foreground_Application")
                    .setAction(new Action(Permission.WRITE, new org.vertx.java.core.Handler<ActionResult>() {
                        @Override
                        public void handle(ActionResult e) {
                            String pkg = Utils.getForegroundActivityPackage();
                            JsonArray results = new JsonArray();
                            JsonObject res = new JsonObject();
                            ValueUtils.toJson(res, "app", new Value(pkg));
                            e.setUpdates(results);
                        }
                    }).addResult(new Parameter("app", ValueType.STRING)))
                    .setDisplayName("Get Foreground Application")
                    .build();
        }

        if (enableNode("battery")) {
            final Node batteryLevelNode = node.createChild("Battery_Level").setValueType(ValueType.NUMBER).build();
            final Node chargerConnectedNode = node.createChild("Charger_Connected").setValueType(ValueType.BOOL).build();
            final Node batteryFullNode = node.createChild("Battery_Full").setValueType(ValueType.BOOL).build();

            batteryFullNode.setDisplayName("Battery Full");
            chargerConnectedNode.setDisplayName("Charger Connected");
            batteryLevelNode.setDisplayName("Battery Level");

            poller(new Executable() {
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

        if (Build.VERSION.SDK_INT >= 20 && enableNode("screen")) {
            setupScreenProvider(node);
        }

        if (enableNode("activity")) {
            activityNode = node.createChild("Activity").setValueType(ValueType.makeEnum(
                    "in_vehicle",
                    "on_bicycle",
                    "running",
                    "walking",
                    "on_foot",
                    "still",
                    "tilting",
                    "unknown"
            )).build();

            activityNode.setValue(new Value("unknown"));

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
            final Node stepsNode = node.createChild("Steps").setValueType(ValueType.NUMBER).build();
            stepsNode.setValue(new Value(0.0));
            stepsNode.setDisplayName("Step Count");

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
            final Node tempCNode = node.createChild("Ambient_Temperature_Celsius").setValueType(ValueType.NUMBER).build();
            final Node tempFNode = node.createChild("Ambient_Temperature_Fahrenheit").setValueType(ValueType.NUMBER).build();

            tempCNode.setDisplayName("Ambient Temperature - Celsius");
            tempFNode.setDisplayName("Ambient Temperature - Fahrenheit");

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
            lux.setValueType(ValueType.NUMBER);

            lux.setDisplayName("Light Level");

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
            final Node pressure = node.createChild("Air_Pressure").setValueType(ValueType.NUMBER).build();

            pressure.setDisplayName("Air Pressure");

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
            final Node humidity = node.createChild("Humidity").setValueType(ValueType.NUMBER).build();

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
            final Node proximity = node.createChild("Proximity").setValueType(ValueType.NUMBER).build();

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
            final Node x = node.createChild("Gyroscope_X").setValueType(ValueType.NUMBER).build();
            final Node y = node.createChild("Gyroscope_Y").setValueType(ValueType.NUMBER).build();
            final Node z = node.createChild("Gyroscope_Z").setValueType(ValueType.NUMBER).build();

            x.setDisplayName("Gyroscope X");
            y.setDisplayName("Gyroscope Y");
            z.setDisplayName("Gyroscope Z");

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

            currentDeviceNode.createChild("Speak").setAction(new Action(Permission.WRITE, new org.vertx.java.core.Handler<ActionResult>() {
                @Override
                public void handle(ActionResult event) {
                    speech.speak(event.getParameter("text").getString(), TextToSpeech.QUEUE_ADD, new HashMap<String, String>());
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
            node.createChild("ShowLocationMap").setDisplayName("Show Location Map").setAction(new Action(Permission.WRITE, new org.vertx.java.core.Handler<ActionResult>() {
                @Override
                public void handle(ActionResult event) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("geo:" + event.getParameter("latitude").getNumber() + "," + event.getParameter("longitude").getNumber()));
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }
                }
            }).addParameter(new Parameter("latitude", ValueType.NUMBER, new Value(0.0))).addParameter(new Parameter("longitude", ValueType.NUMBER, new Value(0.0)))).build();

            node.createChild("ShowMap").setDisplayName("Show Map").setAction(new Action(Permission.WRITE, new org.vertx.java.core.Handler<ActionResult>() {
                @Override
                public void handle(ActionResult event) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);

                    try {
                        intent.setData(Uri.parse("geo:0,0?q=" + URLEncoder.encode(event.getParameter("query").getString(), "UTF-8")));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }
                }
            }).addParameter(new Parameter("query", ValueType.STRING))).build();
        }

        if (preferences.getBoolean("actions.open_url", true)) {
            final Action openUrlAction = new Action(Permission.WRITE, new org.vertx.java.core.Handler<ActionResult>() {

                @Override
                public void handle(ActionResult event) {
                    final Uri url = Uri.parse(event.getParameter("url").getString());
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

            node.createChild("OpenUrl").setDisplayName("Open Url").setAction(openUrlAction).build();
        }

        if (preferences.getBoolean("actions.search", true)) {
            final Action searchAction = new Action(Permission.WRITE, new org.vertx.java.core.Handler<ActionResult>() {
                @Override
                public void handle(ActionResult event) {
                    final String query = event.getParameter("query").getString();
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
            searchAction.addParameter(new Parameter("query", ValueType.STRING));
            node.createChild("Search").setAction(searchAction).build();
        }

        if (enableNode("notifications")) {
            setupNotificationsProvider(node);
        }

        if (enableNode("speech")) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());

            final Node lastSpeechNode = node.createChild("Recognized_Speech").setValueType(ValueType.STRING).build();

            lastSpeechNode.setDisplayName("Recognized Speech");

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
            startSpeechRecognition.setDisplayName("Start Speech Recognition");
            startSpeechRecognition.setAction(startSpeechRecognitionAction);
            Node stopSpeechRecognition = node.createChild("StopSpeechRecognition").build();
            stopSpeechRecognition.setDisplayName("Stop Speech Recognition");
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

                    if (possibles == null) {
                        return;
                    }

                    {
                        float[] sc = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
                        if (sc == null) {
                            return;
                        }

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

    @TargetApi(20)
    private void setupHeartRateMonitor(Node node) {
        Sensor sensor = sensorManager.getDefaultSensor(21);

        if (sensor == null) return;

        final Node rateNode = node.createChild("Heart_Rate").setValueType(ValueType.NUMBER).build();
        rateNode.setDisplayName("Heart Rate");

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
        final Node screenOn = node.createChild("Screen_On").setDisplayName("Screen On").setValueType(ValueType.BOOL).build();

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

    private void setupOpenApplicationProvider(Node node) {
        final Map<String, Intent> LABEL_TO_CLASSES = new HashMap<>();

        Node openAppNode = node.createChild("Open_Application").setAction(
                new Action(Permission.WRITE, new org.vertx.java.core.Handler<ActionResult>() {
                    @Override
                    public void handle(ActionResult result) {
                        String app = result.getParameter("app").getString();
                        startActivity(LABEL_TO_CLASSES.get(app));
                    }
                }).addParameter(new Parameter("app", ValueType.ENUM))
        ).setDisplayName("Open Application").build();

        final JsonObject p = openAppNode.getAction().getParams().get(0);

        Executable updater = new Executable() {
            @Override
            public void run() {
                List<ApplicationInfo> apps = getApplicationContext().getPackageManager().getInstalledApplications(0);
                StringBuilder sb = new StringBuilder();
                for (ApplicationInfo app : apps) {
                    Intent launchIntent = getApplicationContext().getPackageManager().getLaunchIntentForPackage(app.packageName);

                    if (launchIntent == null) {
                        continue;
                    }

                    String label = app.loadLabel(getApplicationContext().getPackageManager()).toString();
                    sb.append(label);
                    LABEL_TO_CLASSES.put(label, launchIntent);
                    sb.append(',');
                }

                if (sb.charAt(sb.length() - 1) == ',') {
                    sb.deleteCharAt(sb.length() - 1);
                }

                p.putString("type", "enum[" + sb.toString() + "]");
            }
        };

        poller(updater).poll(TimeUnit.MINUTES, 1, false);
    }

    private void setupNotificationsProvider(Node node) {
        node
                .createChild("Create_Notification")
                .setAction(new Action(Permission.WRITE, new org.vertx.java.core.Handler<ActionResult>() {
                            @Override
                            public void handle(final ActionResult result) {
                                execute(new Executable() {
                                    @Override
                                    public void run() {
                                        final String title = result.getParameter("title").getString();
                                        final String content = result.getParameter("content").getString();

                                        if (title == null || content == null) {
                                            result.setStreamState(StreamState.CLOSED);
                                            return;
                                        }
                                        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
                                        builder.setSmallIcon(com.dglogik.common.R.mipmap.ic_launcher);
                                        builder.setContentTitle(title);
                                        builder.setContentText(content);
                                        Notification notification = builder.build();
                                        NotificationManagerCompat manager = NotificationManagerCompat.from(getApplicationContext());
                                        int id = currentNotificationId++;
                                        manager.notify(id, notification);
                                        JsonArray updates = new JsonArray();
                                        result.setUpdates(updates);
                                        JsonArray firstUpdate = new JsonArray();
                                        updates.addArray(firstUpdate);
                                        firstUpdate.addObject(new JsonObject().putValue("id", id));
                                        result.setStreamState(StreamState.CLOSED);
                                    }
                                });
                            }
                        }, Action.InvokeMode.ASYNC)
                                .addParameter(new Parameter("title", ValueType.STRING))
                                .addParameter(new Parameter("content", ValueType.STRING))
                                .addResult(new Parameter("id", ValueType.NUMBER))
                ).setDisplayName("Create Notification").build();

        node.createChild("Destroy_Notification")
                .setDisplayName("Destroy Notification")
                .setAction(new Action(Permission.WRITE, new org.vertx.java.core.Handler<ActionResult>() {
                            @Override
                            public void handle(final ActionResult result) {
                                execute(new Executable() {
                                    @Override
                                    public void run() {
                                        Number n = result.getParameter("id").getNumber();
                                        if (n == null) {
                                            result.setStreamState(StreamState.CLOSED);
                                            return;
                                        }
                                        int id = n.intValue();
                                        NotificationManagerCompat manager = NotificationManagerCompat.from(getApplicationContext());
                                        manager.cancel(id);
                                        result.setStreamState(StreamState.CLOSED);
                                    }
                                });
                            }
                        }, Action.InvokeMode.ASYNC).addParameter(new Parameter("id", ValueType.NUMBER))
                ).build();
    }

    public boolean enableSensor(String id, int type) {
        return enableNode(id) && !sensorManager.getSensorList(type).isEmpty();
    }

    public int currentNotificationId = 1;

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

        if (link != null) {
            link.stop();
        }

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
