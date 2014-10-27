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

public class GyroscopeProvider extends Provider {
    private SensorEventListener eventListener;

    @Override
    public String name() {
        return "Gyroscope";
    }

    @Override
    public void setup() {
        eventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(final SensorEvent sensorEvent) {
                update(new HashMap<String, Object>() {{
                    put("X", sensorEvent.values[0]);
                    put("Y", sensorEvent.values[1]);
                    put("Z", sensorEvent.values[2]);
                }});
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };

        LinkService.INSTANCE.sensorManager.registerListener(eventListener, LinkService.INSTANCE.sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public boolean supported() {
        return LinkService.INSTANCE.sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null;
    }

    @Override
    public Map<String, Integer> valueTypes() {
        return new HashMap<String, Integer>() {{
            put("X", ValueType.NUMBER);
            put("Y", ValueType.NUMBER);
            put("Z", ValueType.NUMBER);
        }};
    }

    @Override
    public void destroy() {
        LinkService.INSTANCE.sensorManager.unregisterListener(eventListener);
    }
}
