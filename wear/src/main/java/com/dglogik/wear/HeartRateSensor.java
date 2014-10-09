package com.dglogik.wear;

import android.hardware.SensorEvent;

public class HeartRateSensor extends SensorActivity {
    @Override
    public String getName() {
        return "Heart Rate";
    }

    @Override
    public void onSetup() {
        registerSensor(getSensorByName("Wellness Passive Sensor"));
    }

    @Override
    public float collect(SensorEvent event) {
        return event.values[2];
    }
}
 