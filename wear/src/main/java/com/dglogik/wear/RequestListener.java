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
            doInit(messageEvent.getSourceNodeId());
        } else if (messageEvent.getPath().equals("/wear/action")) {
        	byte[] bytes = messageEvent.getData();
            String content = new String(bytes);
            try {
                JSONObject data = new JSONObject(new JSONTokener(content));

                String actionName = data.getString("action");

                List<Action> allActions = LinkService.INSTANCE.actions;

                for (final Action action : allActions) {
                    if (actionName.equals(action.getName())) {
                        Utils.log("Invoking Action " + actionName);
                        action.invoke();
                        Utils.log("Invoked Action " + actionName);
                        LinkService.INSTANCE.sendSingle(messageEvent.getSourceNodeId(), "action-invoked", new HashMap<String, Object>() {{
                            put("action", action.getName());
                        }});
                        return;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void doInit(String node) {
        if (LinkService.INSTANCE.connected.contains(node)) {
            return;
        }
        LinkService.INSTANCE.connected.add(node);
        List<Provider> providers = LinkService.INSTANCE.providers;
        final HashMap<String, Map<String, Integer>> points = new HashMap<>();

        for (Provider provider : providers) {
            points.put(provider.name(), provider.valueTypes());
        }

        final List<String> actions = new ArrayList<>();

        for (Action action : LinkService.INSTANCE.actions) {
            actions.add(action.getName());
        }

        try {
            LinkService.INSTANCE.sendSingle(node, "points", new HashMap<String, Object>() {{
                put("points", points);
                put("actions", actions);
            }});

            Thread.sleep(500);

            for (final Provider provider : LinkService.INSTANCE.providers) {
                if (provider.supported()) {
                    final Map<String, Object> values = provider.currentValues;

                    LinkService.INSTANCE.sendSingle(node, "update", new HashMap<String, Object>() {{
                        put("values", values);
                        put("point", provider.name());
                    }});
                }
            }
        } catch (JSONException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
