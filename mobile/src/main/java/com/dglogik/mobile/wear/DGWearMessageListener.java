package com.dglogik.mobile.wear;

import android.support.annotation.NonNull;

import com.dglogik.mobile.DGMobileContext;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.NodeBuilder;
import org.dsa.iot.dslink.node.Permission;
import org.dsa.iot.dslink.node.actions.Action;
import org.dsa.iot.dslink.node.actions.ActionResult;
import org.dsa.iot.dslink.node.value.Value;
import org.dsa.iot.dslink.node.value.ValueType;
import org.dsa.iot.dslink.node.value.ValueUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.vertx.java.core.Handler;

import java.util.HashMap;
import java.util.HashSet;
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
                    Node devicesNode = DGMobileContext.CONTEXT.devicesNode;

                    if (devicesNode.getChildren().containsKey(event.getSourceNodeId())) {
                        devicesNode.removeChild(event.getSourceNodeId());
                    }

                    Node deviceNode = devicesNode.createChild(event.getSourceNodeId()).setDisplayName(device).build();

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

                            if (deviceNode.getChildren().containsKey(id)) {
                                deviceNode.removeChild(id);
                            }

                            NodeBuilder builder = deviceNode.createChild(id);

                            switch (valueType) {
                                case 0:
                                    builder.setValueType(ValueType.STRING);
                                    break;
                                case 1:
                                    builder.setValueType(ValueType.NUMBER);
                                    break;
                                case 2:
                                    builder.setValueType(ValueType.BOOL);
                                    break;
                                default:
                                    throw new IllegalArgumentException();
                            }

                            Node node = builder.build();

                            dataNodes.put(event.getSourceNodeId() + "@" + id, node);
                        }
                    }

                    for (int i = 0; i < actions.length(); i++) {
                        final String name = actions.getString(i);

                        final Action action = new Action(Permission.WRITE, new Handler<ActionResult>() {
                            @Override
                            public void handle(ActionResult actionResult) {
                                JSONObject object = new JSONObject();
                                try {
                                    object.put("action", name);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                Wearable.MessageApi.sendMessage(DGMobileContext.CONTEXT.googleClient, event.getSourceNodeId(), "/wear/action", object.toString().getBytes());
                            }
                        });

                        if (deviceNode.getChildren().containsKey(name)) {
                            deviceNode.removeChild(name);
                        }

                        deviceNode.createChild(name).setAction(action).build();
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

                        Node node = dataNodes.get(event.getSourceNodeId() + "@" + id);

                        if (node == null) {
                            DGMobileContext.log("ERROR: Node not found: " + event.getSourceNodeId() + "@" + id);
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
                    HashSet<String> keys = new HashSet<>(dataNodes.keySet());
                    for (String key : keys) {
                        if (key.startsWith(event.getSourceNodeId() + "@")) {
                            dataNodes.remove(key);
                        }
                    }
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
