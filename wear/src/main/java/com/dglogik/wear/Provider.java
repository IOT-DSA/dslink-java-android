package com.dglogik.wear;

import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;

public abstract class Provider {
    public Map<String, Object> currentValues;

    public abstract String name();
    public abstract void setup();
    public abstract boolean supported();
    public abstract Map<String, Integer> valueTypes();

    public void update(final Map<String, Object> values) {
        currentValues = values;

        try {
            LinkService.INSTANCE.send("update", new HashMap<String, Object>() {{
                put("values", values);
                put("point", name());
            }});
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public GoogleApiClient googleClient() {
        return LinkService.INSTANCE.googleClient;
    }

    public void setInitialValues() {}

    public abstract void destroy();
}
