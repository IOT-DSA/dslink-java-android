package com.dglogik.mobile;

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

import org.eclipse.jetty.server.Server;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
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
                try {
                    Field field = link.getClass().getDeclaredField("server");
                    Server server = (Server) field.get(link);
                    if (server != null) {
                        server.stop();
                    }
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        link.setClient(client);
    }

    public void initialize() {
        if (preferences.getBoolean("feature.wearable", true)) {
            wearable.initialize();
        }

        DeviceNode device = new DeviceNode(Build.MODEL);
        setupCurrentDevice(device);
        rootNode.addChild(device);

        startLink();
    }

    public final Timer timer = new Timer();

    public double lastLatitude;
    public double lastLongitude;

    public void setupCurrentDevice(@NonNull DeviceNode node) {
        final DisplayManager displayManager = (DisplayManager) service.getSystemService(Context.DISPLAY_SERVICE);

        if (preferences.getBoolean("providers.location", true)) {
            final DataValueNode latitudeNode = new DataValueNode("Location_Latitude", BasicMetaData.SIMPLE_INT);
            final DataValueNode longitudeNode = new DataValueNode("Location_Longitude", BasicMetaData.SIMPLE_INT);

            LocationRequest request = new LocationRequest();

            request.setFastestInterval(1000);
            request.setInterval(3000);
            request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            LocationServices.FusedLocationApi.requestLocationUpdates(googleClient, request, new LocationListener() {
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

        if (preferences.getBoolean("providers.screen", true)) {
            final DataValueNode screenOn = new DataValueNode("Screen_On", BasicMetaData.SIMPLE_BOOL);

            displayManager.registerDisplayListener(new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int i) {
                }

                @Override
                public void onDisplayRemoved(int i) {
                }

                @Override
                public void onDisplayChanged(int i) {
                    boolean on = displayManager.getDisplay(i).getState() == Display.STATE_ON;
                    screenOn.update(on);
                }
            }, new Handler());

            node.addChild(screenOn);
        }

        if (preferences.getBoolean("actions.notifications", true)) {
            final NotificationManager notificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);

            final BaseAction createNotificationAction = new BaseAction("createNotification") {
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

            final BaseAction destroyNotificationAction = new BaseAction("destroyNotification") {
                @NonNull
                @Override
                public Map<String, DGValue> invoke(BaseNode baseNode, @NonNull Map<String, DGValue> args) {
                    int id = args.get("id").toInt();

                    notificationManager.cancel(id);

                    return new HashMap<String, DGValue>();
                }
            };

            destroyNotificationAction.addParam("id", BasicMetaData.SIMPLE_INT);

            node.addAction(createNotificationAction);
            node.addAction(destroyNotificationAction);
        }

        if (enableSensor(Sensor.TYPE_STEP_COUNTER, "steps")) {
            final DataValueNode stepsNode = new DataValueNode("Steps", BasicMetaData.SIMPLE_INT);
            Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

            sensorManager.registerListener(new SensorEventListener() {
                @Override
                public void onSensorChanged(@NonNull SensorEvent event) {
                    stepsNode.update((double) event.values[0]);
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }
            }, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        if (enableSensor(Sensor.TYPE_HEART_RATE, "heart_rate")) {
            final DataValueNode rateNode = new DataValueNode("Heart_Rate", BasicMetaData.SIMPLE_INT);
            Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

            sensorManager.registerListener(new SensorEventListener() {
                @Override
                public void onSensorChanged(@NonNull SensorEvent event) {
                    rateNode.update((double) event.values[0]);
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }
            }, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        if (enableSensor(Sensor.TYPE_AMBIENT_TEMPERATURE, "ambient_temp")) {
            final DataValueNode tempCNode = new DataValueNode("Ambient_Temperature_Celsius", BasicMetaData.SIMPLE_INT);
            final DataValueNode tempFNode = new DataValueNode("Ambient_Temperature_Fahrenheit", BasicMetaData.SIMPLE_INT);

            Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);

            sensorManager.registerListener(new SensorEventListener() {
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
            }, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        if (enableSensor(Sensor.TYPE_LIGHT, "light")) {
            final DataValueNode lux = new DataValueNode("Light_Level", BasicMetaData.SIMPLE_INT);

            Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

            sensorManager.registerListener(new SensorEventListener() {
                @Override
                public void onSensorChanged(@NonNull SensorEvent event) {
                    lux.update((double) event.values[0]);
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }
            }, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        if (enableSensor(Sensor.TYPE_PRESSURE, "air_pressure")) {
            final DataValueNode pressure = new DataValueNode("Air_Pressure", BasicMetaData.SIMPLE_INT);

            Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);

            sensorManager.registerListener(new SensorEventListener() {
                @Override
                public void onSensorChanged(@NonNull SensorEvent event) {
                    pressure.update((double) event.values[0]);
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }
            }, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        if (enableSensor(Sensor.TYPE_RELATIVE_HUMIDITY, "humidity")) {
            final DataValueNode humidity = new DataValueNode("Humidity", BasicMetaData.SIMPLE_INT);

            Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);

            sensorManager.registerListener(new SensorEventListener() {
                @Override
                public void onSensorChanged(@NonNull SensorEvent event) {
                    humidity.update((double) event.values[0]);
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }
            }, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        if (enableSensor(Sensor.TYPE_PROXIMITY, "proximity")) {
            final DataValueNode proximity = new DataValueNode("Proximity", BasicMetaData.SIMPLE_INT);

            Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

            sensorManager.registerListener(new SensorEventListener() {
                @Override
                public void onSensorChanged(@NonNull SensorEvent event) {
                    proximity.update((double) event.values[0]);
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }
            }, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        if (enableSensor(Sensor.TYPE_GYROSCOPE, "gyroscope")) {
            final DataValueNode x = new DataValueNode("Gyroscope_X", BasicMetaData.SIMPLE_INT);
            final DataValueNode y = new DataValueNode("Gyroscope_Y", BasicMetaData.SIMPLE_INT);
            final DataValueNode z = new DataValueNode("Gyroscope_Z", BasicMetaData.SIMPLE_INT);


            Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

            sensorManager.registerListener(new SensorEventListener() {
                @Override
                public void onSensorChanged(@NonNull SensorEvent event) {
                    x.update((double) event.values[0]);
                    y.update((double) event.values[1]);
                    z.update((double) event.values[2]);
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }
            }, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public boolean enableSensor(int type, String name) {
        return !sensorManager.getSensorList(type).isEmpty() && preferences.getBoolean("providers.sensors." + name, true);
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

    public void start() {
        googleClient.connect();
    }

    public void destroy() {
        googleClient.disconnect();
        client.stop();
    }

    public void log(String message) {
        Log.i(TAG, message);
    }
}
