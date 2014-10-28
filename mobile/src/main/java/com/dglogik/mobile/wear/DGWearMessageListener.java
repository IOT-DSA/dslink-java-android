package com.dglogik.mobile.wear;

import android.support.annotation.NonNull;

import com.dglogik.api.BasicMetaData;
import com.dglogik.api.DGContext;
import com.dglogik.api.DGMetaData;
import com.dglogik.dslink.node.base.BaseAction;
import com.dglogik.dslink.node.base.BaseNode;
import com.dglogik.mobile.DGMobileContext;
import com.dglogik.mobile.LinkService;
import com.dglogik.mobile.link.DataValueNode;
import com.dglogik.mobile.link.DeviceNode;
import com.dglogik.value.DGValue;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DGWearMessageListener implements MessageApi.MessageListener {
    @NonNull
    private Map<String, DataValueNode> dataNodes = new HashMap<String, DataValueNode>();

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

            if (type.equals("points")) {
                DGMobileContext.CONTEXT.wearable.namesMap.put(event.getSourceNodeId(), device);
                DeviceNode deviceNode = new DeviceNode(device);
                DGMobileContext.CONTEXT.rootNode.addChild(deviceNode);
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

                        DGMetaData realType;

                        switch (valueType) {
                            case 0:
                                realType = BasicMetaData.SIMPLE_STRING;
                                break;
                            case 1:
                                realType = BasicMetaData.SIMPLE_INT;
                                break;
                            case 2:
                                realType = BasicMetaData.SIMPLE_BOOL;
                                break;
                            default:
                                throw new IllegalArgumentException();
                        }

                        DataValueNode node = new DataValueNode(id, realType);

                        dataNodes.put(device + "@" + id, node);

                        deviceNode.addChild(node);
                    }
                }

                for (int i = 0; i < actions.length(); i++) {
                    final String name = actions.getString(i);

                    final BaseAction action = new BaseAction(name) {
                        @Override
                        public Map<String, DGValue> invoke(BaseNode baseNode, Map<String, DGValue> args) {
                            JSONObject object = new JSONObject();
                            try {
                                object.put("action", name);
                            } catch (JSONException e) {
                                e.printStackTrace();
                                return new HashMap<String, DGValue>();
                            }
                            Wearable.MessageApi.sendMessage(DGMobileContext.CONTEXT.googleClient, event.getSourceNodeId(), "/wear/action", object.toString().getBytes());
                            return new HashMap<String, DGValue>();
                        }
                    };

                    deviceNode.addAction(action);
                }

                if (!DGMobileContext.CONTEXT.linkStarted) {
                    DGMobileContext.CONTEXT.startLink();
                }
            } else if (type.equals("update")) {
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

                    DataValueNode node = dataNodes.get(device + "@" + id);

                    if (node == null) {
                        DGMobileContext.CONTEXT.log("ERROR: Node not found: " + device + "@" + id);
                        continue;
                    }

                    node.update(values.get(name));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
