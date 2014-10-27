package com.dglogik.wear.providers;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.view.Display;

import com.dglogik.wear.LinkService;
import com.dglogik.wear.Provider;
import com.dglogik.wear.ValueType;

import java.util.HashMap;
import java.util.Map;

public class ScreenProvider extends Provider {
    private DisplayManager displayManager;
    private DisplayManager.DisplayListener displayListener;

    @Override
    public String name() {
        return "Screen";
    }

    @Override
    public void setup() {
        displayManager = (DisplayManager) LinkService.INSTANCE.getSystemService(Context.DISPLAY_SERVICE);
        displayListener = new DisplayManager.DisplayListener() {
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
        };
        displayManager.registerDisplayListener(displayListener, new Handler());
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

    @Override
    public void destroy() {
        displayManager.unregisterDisplayListener(displayListener);
    }
}
