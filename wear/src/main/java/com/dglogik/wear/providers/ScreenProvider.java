package com.dglogik.wear.providers;

import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.view.Display;

import com.dglogik.wear.MainActivity;
import com.dglogik.wear.Provider;
import com.dglogik.wear.ValueType;

import java.util.HashMap;
import java.util.Map;

public class ScreenProvider extends Provider {
    @Override
    public String name() {
        return "Screen";
    }

    @Override
    public void setup() {
        final DisplayManager displayManager = (DisplayManager) MainActivity.INSTANCE.getSystemService(MainActivity.DISPLAY_SERVICE);
        displayManager.registerDisplayListener(new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayAdded(int i) {
            }

            @Override
            public void onDisplayRemoved(int i) {
            }

            @Override
            public void onDisplayChanged(int i) {
                final int state = displayManager.getDisplay(i).getState();

                update(new HashMap<String, Object>() {{
                    put("On", state == Display.STATE_ON);
                }});
            }
        }, new Handler());
    }

    @Override
    public boolean supported() {
        return true;
    }

    @Override
    public Map<String, Integer> valueTypes() {
        return new HashMap<String, Integer>() {{
            put("On", ValueType.BOOLEAN);
        }};
    }
}
