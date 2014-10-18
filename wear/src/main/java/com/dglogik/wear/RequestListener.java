package com.dglogik.wear;

import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;

import org.json.JSONException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestListener implements MessageApi.MessageListener {
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals("/wear/init")) {
            List<Provider> providers = MainActivity.INSTANCE.providers;
            final HashMap<String, Map<String, Integer>> points = new HashMap<String, Map<String, Integer>>();

            for (Provider provider : providers) {
                points.put(provider.name(), provider.valueTypes());
            }

            try {
                MainActivity.INSTANCE.sendSingle(messageEvent.getSourceNodeId(), "points", new HashMap<String, Object>() {{
                    put("points", points);
                }});
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
