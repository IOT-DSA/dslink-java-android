package com.dglogik.wear.providers;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.dglogik.wear.MainActivity;
import com.dglogik.wear.Provider;
import com.dglogik.wear.ValueType;

import java.util.HashMap;
import java.util.Map;

public class HealthProvider extends Provider {
    private double lastHeartRate = -1.0;

    @Override
    public String name() {
        return "Health";
    }

    @Override
    public void setup() {
        final SensorManager sensorManager = MainActivity.INSTANCE.sensorManager;

        Sensor heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        sensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                final double heartRate = sensorEvent.values[0];

                if (lastHeartRate != heartRate) {
                    update(new HashMap<String, Object>() {{
                        put("HeartRate", heartRate);
                    }});
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        }, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public boolean supported() {
        return MainActivity.INSTANCE.sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) != null;
    }

    @Override
    public Map<String, Integer> valueTypes() {
        return new HashMap<String, Integer>() {{
            put("HeartRate", ValueType.NUMBER);
        }};
    }
}
