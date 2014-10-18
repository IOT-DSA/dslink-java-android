package com.dglogik.mobile.wear;

import com.dglogik.mobile.DGMobileContext;
import com.dglogik.mobile.link.SensorNode;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;

import org.eclipse.jetty.util.MultiMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.List;
import java.util.Map;

public class DGWearMessageListener implements MessageApi.MessageListener {
    @Override
    public void onMessageReceived(MessageEvent event) {
        try {
            String path = event.getPath();

            if (!path.equals("/wear")) {
                return;
            }

            byte[] bytes = event.getData();

            String content = new String(bytes);

            JSONObject data = new JSONObject(new JSONTokener(content));
            String type = data.getString("type");

            if (type.equals("sensors")) {
                JSONArray sensorNames = data.getJSONArray("sensors");

                DGMobileContext.CONTEXT.wearable.sensorNames.clear();

                for (int i = 0; i < sensorNames.length(); i++) {
                    DGMobileContext.CONTEXT.wearable.sensorNames.add(sensorNames.getString(i));
                }

                System.out.println("Watch has given us the sensor list.");

                DGMobileContext.CONTEXT.startLink();
            } else if (type.equals("update")) {
                String nodeName = data.getString("node");
                double value = data.getDouble("value");
                Map<String, SensorNode> nodes = DGMobileContext.CONTEXT.wearable.nodes;

                if (nodes.containsKey(nodeName)) {
                    nodes.get(nodeName).update(value);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
