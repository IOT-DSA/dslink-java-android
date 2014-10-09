package com.dglogik.wear;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

public abstract class SensorActivity extends Activity implements SensorEventListener {
    protected SensorManager mSensorManager;

    public SensorActivity() {
    }

    public abstract void onSetup();

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        onSetup();
    }

    public void registerSensor(Sensor sensor) {
        mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public Sensor getDefaultSensor(int type) {
        return mSensorManager.getDefaultSensor(type);
    }

    public Sensor getSensorByName(String name) {
        for (Sensor sensor : mSensorManager.getSensorList(Sensor.TYPE_ALL)) {
            if (sensor.getName().equals(name)) {
                return sensor;
            }
        }
        return null;
    }

    public abstract void collect(SensorEvent event);

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        System.out.println("Accuracy Update for " + sensor.getName());
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        System.out.println("Sensor Changed: " + event.sensor.getName());
        for (float value : event.values) {
            System.out.println("Value: " + value);
        }
    }
}
 