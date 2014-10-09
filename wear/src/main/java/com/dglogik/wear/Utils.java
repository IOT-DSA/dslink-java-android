package com.dglogik.wear;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.util.Log;

public class Utils {
    public static void printSensorList(SensorManager manager) {
        for (Sensor sensor : manager.getSensorList(Sensor.TYPE_ALL)) {
            Log.i("DGWear", "Sensor: " + sensor.getName() + " by " + sensor.getVendor() + " (version: " + sensor.getVersion() + ", type: " + sensor.getStringType() + ")");
        }
    }
}
