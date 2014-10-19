package com.dglogik.mobile;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.eclipse.jetty.server.Server;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class DGMobileContext {
    public static final String TAG = "DGWear";
    public static DGMobileContext CONTEXT;

    public final LinkService service;
    public final WearableSupport wearable;
    public final GoogleApiClient googleClient;
    public final Application link;
    public boolean linkStarted = false;
    public final RootNode rootNode;
    public final SensorManager sensorManager;
    public final LocationManager locationManager;
    public final Client client;

    public DGMobileContext(final LinkService service) {
        CONTEXT = this;
        this.service = service;
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
        wearable.initialize();
        DeviceNode device = new DeviceNode(Build.MODEL);
        setupCurrentDevice(device);
        rootNode.addChild(device);
        startLink();
    }

    public final Timer timer = new Timer();

    public double lastLatitude;
    public double lastLongitude;

    public void setupCurrentDevice(DeviceNode node) {

        {
            final DataValueNode latitudeNode = new DataValueNode("Location_Latitude", BasicMetaData.SIMPLE_INT);
            final DataValueNode longitudeNode = new DataValueNode("Location_Longitude", BasicMetaData.SIMPLE_INT);

            LocationRequest request = new LocationRequest();

            request.setFastestInterval(1000);
            request.setInterval(3000);

            LocationServices.FusedLocationApi.requestLocationUpdates(googleClient, request, new com.google.android.gms.location.LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
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

        {
            final DataValueNode batteryLevelNode = new DataValueNode("Battery_Level", BasicMetaData.SIMPLE_INT);
            final DataValueNode chargerConnectedNode = new DataValueNode("Charger_Connected", BasicMetaData.SIMPLE_BOOL);
            final DataValueNode batteryFullNode = new DataValueNode("Battery_Full", BasicMetaData.SIMPLE_BOOL);
            final Intent batteryStatus = getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

            boolean 

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

        {
            final DataValueNode screenOn = new DataValueNode("Screen_On", BasicMetaData.SIMPLE_BOOL);
            final DisplayManager displayManager = (DisplayManager) service.getSystemService(Context.DISPLAY_SERVICE);

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

        {
            final NotificationManager notificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);

            final BaseAction createNotificationAction = new BaseAction("createNotification") {
                @Override
                public Map<String, DGValue> invoke(BaseNode baseNode, Map<String, DGValue> args) {
                    Notification.Builder builder = new Notification.Builder(getApplicationContext());

                    builder.setContentTitle(args.get("title").toString());
                    builder.setContentText(args.get("content").toString());

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
                @Override
                public Map<String, DGValue> invoke(BaseNode baseNode, Map<String, DGValue> args) {
                    int id = args.get("id").toInt();

                    notificationManager.cancel(id);

                    return new HashMap<String, DGValue>();
                }
            };

            destroyNotificationAction.addParam("id", BasicMetaData.SIMPLE_INT);

            node.addAction(createNotificationAction);
            node.addAction(destroyNotificationAction);
        }
    }

    public int currentNotificationId = 0;

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
        return service.getApplicationContext();
    }

    public void start() {
        googleClient.connect();
    }

    public void destroy() {
        googleClient.disconnect();
        client.stop();
    }
}
