package com.dglogik.wear.providers;

import android.app.admin.DeviceAdminInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;

import com.dglogik.wear.MainActivity;
import com.dglogik.wear.Provider;
import com.dglogik.wear.ValueType;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class DeviceProvider extends Provider {
    @Override
    public String name() {
        return "Device";
    }

    @Override
    public void setup() {
        Timer timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                update(new HashMap<String, Object>() {{
                    put("Release", Build.VERSION.RELEASE);
                    put("SDK_Version", Build.VERSION.SDK_INT);
                    put("Release_Codename", Build.VERSION.CODENAME);
                    put("Board", Build.BOARD);
                    put("Brand", Build.BRAND);
                    put("Model", Build.MODEL);
                    put("Manufacturer", Build.MANUFACTURER);
                    put("Type", Build.TYPE);
                }});
            }
        }, 2000, 10000);
    }

    @Override
    public boolean supported() {
        return true;
    }

    @Override
    public Map<String, Integer> valueTypes() {
        return new HashMap<String, Integer>() {{
            put("Release", ValueType.STRING);
            put("SDK_Version", ValueType.NUMBER);
            put("Release_Codename", ValueType.STRING);
            put("Board", ValueType.STRING);
            put("Brand", ValueType.STRING);
            put("Model", ValueType.STRING);
            put("Manufacturer", ValueType.STRING);
            put("Type", ValueType.STRING);
        }};
    }
}
