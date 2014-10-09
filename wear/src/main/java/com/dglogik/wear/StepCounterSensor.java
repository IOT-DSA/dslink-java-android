package com.dglogik.wear;

import android.hardware.SensorEvent;

public class StepCounterSensor extends SensorActivity {
    @Override
    public String getName() {
        return "Step Counter";
    }

    @Override
    public void onSetup() {
        registerSensor(getSensorByName("Detailed Step Counter Sensor"));
    }

    @Override
    public float collect(SensorEvent event) {
        return event.values[0];
    }
}
 