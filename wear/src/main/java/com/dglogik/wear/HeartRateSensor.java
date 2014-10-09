package com.dglogik.wear;

import android.hardware.SensorEvent;

public class HeartRateSensor extends SensorActivity {
    @Override
    public void onSetup() {
        registerSensor(getSensorByName("Wellness Passive Sensor"));
    }

    @Override
    public void collect(SensorEvent event) {
        float rate = event.values[2];
    }
}
 