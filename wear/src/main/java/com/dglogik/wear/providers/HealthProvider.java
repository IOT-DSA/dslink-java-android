package com.dglogik.wear.providers;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.dglogik.wear.LinkService;
import com.dglogik.wear.Provider;
import com.dglogik.wear.ValueType;

import java.util.HashMap;
import java.util.Map;

public class HealthProvider extends Provider {
    private SensorEventListener eventListener;
    private double lastHeartRate = 0.0;

    @Override
    public String name() {
        return "Health";
    }

    @Override
    public void setup() {
        final SensorManager sensorManager = LinkService.INSTANCE.sensorManager;

        Sensor heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        eventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                final double heartRate = sensorEvent.values[0];

                if (lastHeartRate != heartRate) {
                    update(new HashMap<String, Object>() {{
                        put("HeartRate", heartRate);
                    }});

                    lastHeartRate = heartRate;
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };

        sensorManager.registerListener(eventListener, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void setInitialValues() {
        update(new HashMap<String, Object>() {{
            put("HeartRate", 0.0);
        }});
    }

    @Override
    public boolean supported() {
        return LinkService.INSTANCE.sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) != null;
    }

    @Override
    public Map<String, Integer> valueTypes() {
        return new HashMap<String, Integer>() {{
            put("HeartRate", ValueType.NUMBER);
        }};
    }

    @Override
    public void destroy() {
        LinkService.INSTANCE.sensorManager.unregisterListener(eventListener);
    }
}
