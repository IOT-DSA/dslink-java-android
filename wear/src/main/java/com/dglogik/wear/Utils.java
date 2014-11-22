package com.dglogik.wear;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.support.annotation.NonNull;
import android.util.Log;

public class Utils {
    public static final String TAG = "DGWear";

    public static void log(String message) {
        Log.i(TAG, message);
    }

    public static void printSensorList(SensorManager manager) {
        for (Sensor sensor : manager.getSensorList(Sensor.TYPE_ALL)) {
            log("Sensor: " + sensor.getName() + " by " + sensor.getVendor() + " (version: " + sensor.getVersion() + ", type: " + sensor.getStringType() + ")");
        }
    }
}
