package com.dglogik.mobile.wear;

import android.support.annotation.NonNull;

import com.dglogik.mobile.DGMobileContext;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.dsa.iot.dslink.responder.action.Action;
import org.dsa.iot.dslink.util.Permission;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DGWearMessageListener implements MessageApi.MessageListener {
    @NonNull
    private final Map<String, Node> dataNodes = new HashMap<>();

    @Override
    public void onMessageReceived(@NonNull final MessageEvent event) {
        try {
            String path = event.getPath();

            if (!path.equals("/wear")) {
                return;
            }

            byte[] bytes = event.getData();

            final String content = new String(bytes);

            JSONObject data = new JSONObject(new JSONTokener(content));

            String type = data.getString("type");
            String device = data.getString("device");

            DGMobileContext.log("Wearable " + device + " sent " + type);

            switch (type) {
                case "points": {
                    DGMobileContext.CONTEXT.wearable.wearNodes.add(event.getSourceNodeId());
                    DGMobileContext.CONTEXT.wearable.namesMap.put(event.getSourceNodeId(), device);
                    Node deviceNode = DGMobileContext.CONTEXT.devicesNode.createChild(device);

                    JSONObject points = data.getJSONObject("points");
                    JSONArray actions = data.getJSONArray("actions");

                    Iterator names = points.keys();

                    while (names.hasNext()) {
                        String pointName = (String) names.next();
                        JSONObject pointValues = points.getJSONObject(pointName);

                        Iterator pointValueNames = pointValues.keys();

                        while (pointValueNames.hasNext()) {
                            String pointValueName = (String) pointValueNames.next();
                            int valueType = pointValues.getInt(pointValueName);
                            String id;
                            if (pointValues.length() == 1 && pointValueName.equals("value")) {
                                id = pointName;
                            } else {
                                id = pointName + "_" + pointValueName;
                            }

                            ValueType realType;

                            switch (valueType) {
                                case 0:
                                    realType = ValueType.STRING;
                                    break;
                                case 1:
                                    realType = ValueType.NUMBER;
                                    break;
                                case 2:
                                    realType = ValueType.BOOL;
                                    break;
                                default:
                                    throw new IllegalArgumentException();
                            }

                            Node node = deviceNode.createChild(pointName);

                            node.setConfiguration("type", new Value(realType.toJsonString()));

                            dataNodes.put(device + "@" + id, node);

                            deviceNode.addChild(node);
                        }
                    }

                    for (int i = 0; i < actions.length(); i++) {
                        final String name = actions.getString(i);

                        final Action action = new Action(Permission.WRITE, new Handler<JsonObject>() {
                            @Override
                            public void handle(JsonObject e) {
                                JSONObject object = new JSONObject();

                                try {
                                    object.put("action", name);
                                } catch (JSONException err) {
                                    err.printStackTrace();
                                }

                                Wearable.MessageApi.sendMessage(DGMobileContext.CONTEXT.googleClient, event.getSourceNodeId(), "/wear/action", object.toString().getBytes());
                            }
                        });

                        Node actionNode = deviceNode.createChild(name);
                        actionNode.setAction(action);
                    }

                    if (!DGMobileContext.CONTEXT.linkStarted) {
                        DGMobileContext.CONTEXT.startLink();
                    }
                    break;
                }
                case "update": {
                    String pointName = data.getString("point");

                    JSONObject values = data.getJSONObject("values");

                    Iterator names = values.keys();

                    while (names.hasNext()) {
                        String name = (String) names.next();

                        String id;

                        if (values.length() == 1 && name.equals("value")) {
                            id = pointName;
                        } else {
                            id = pointName + "_" + name;
                        }

                        Node node = dataNodes.get(device + "@" + id);

                        if (node == null) {
                            DGMobileContext.log("ERROR: Node not found: " + device + "@" + id);
                            continue;
                        }

                        Value value = ValueUtils.toValue(values.get(name));

                        node.setValue(value);
                    }
                    break;
                }
                case "ready":
                    Wearable.MessageApi.sendMessage(DGMobileContext.CONTEXT.googleClient, event.getSourceNodeId(), "/wear/init", null);
                    break;
                case "stop":
                    String name = DGMobileContext.CONTEXT.wearable.namesMap.get(event.getSourceNodeId());
                    DGMobileContext.CONTEXT.devicesNode.removeChild(name);
                    DGMobileContext.CONTEXT.wearable.wearNodes.remove(event.getSourceNodeId());
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
