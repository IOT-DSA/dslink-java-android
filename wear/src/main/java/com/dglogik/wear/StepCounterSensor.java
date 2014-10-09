package com.dglogik.wear;

import android.hardware.SensorEvent;

public class StepCounterSensor extends SensorActivity {
    @Override
    public void onSetup() {
        registerSensor(getSensorByName("Detailed Step Counter Sensor"));
    }

    @Override
    public void collect(SensorEvent event) {
        float steps = event.values[0];
    }
}
 