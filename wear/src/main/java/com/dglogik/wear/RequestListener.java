package com.dglogik.wear;

import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestListener implements MessageApi.MessageListener {
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Utils.log("Wearable Message Received on path " + messageEvent.getPath());
        if (messageEvent.getPath().equals("/wear/init")) {
            List<Provider> providers = LinkService.INSTANCE.providers;
            final HashMap<String, Map<String, Integer>> points = new HashMap<String, Map<String, Integer>>();

            for (Provider provider : providers) {
                points.put(provider.name(), provider.valueTypes());
            }

            final List<String> actions = new ArrayList<String>();

            for (Action action : LinkService.INSTANCE.actions) {
                actions.add(action.getName());
            }

            try {
                LinkService.INSTANCE.sendSingle(messageEvent.getSourceNodeId(), "points", new HashMap<String, Object>() {{
                    put("points", points);
                    put("actions", actions);
                }});
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if (messageEvent.getPath().equals("/wear/action")) {
        	byte[] bytes = messageEvent.getData();
            String content = new String(bytes);
            try {
                JSONObject data = new JSONObject(new JSONTokener(content));

                String actionName = data.getString("action");

                List<Action> allActions = LinkService.INSTANCE.actions;

                for (Action action : allActions) {
                    if (actionName.equals(action.getName())) {
                        action.invoke();
                        return;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
