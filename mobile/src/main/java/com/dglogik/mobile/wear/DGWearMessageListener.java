package com.dglogik.mobile.wear;

import com.dglogik.api.BasicMetaData;
import com.dglogik.api.DGMetaData;
import com.dglogik.mobile.DGMobileContext;
import com.dglogik.mobile.link.DataValueNode;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.Iterator;
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

            //System.out.println(data.toString(2));

            String type = data.getString("type");

            if (type.equals("points")) {
                JSONObject points = data.getJSONObject("points");

                Iterator<String> names = points.keys();

                while (names.hasNext()) {
                    String pointName = names.next();
                    JSONObject pointValues = points.getJSONObject(pointName);

                    Iterator<String> pointValueNames = pointValues.keys();

                    while (pointValueNames.hasNext()) {
                        String pointValueName = pointValueNames.next();
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

                        DGMobileContext.CONTEXT.rootNode.addChild(new DataValueNode(id, realType));
                    }
                }

                if (!DGMobileContext.CONTEXT.linkStarted) {
                    DGMobileContext.CONTEXT.startLink();
                }
            } else if (type.equals("update")) {
                String pointName = data.getString("point");

                JSONObject values = data.getJSONObject("values");

                Iterator<String> names = values.keys();

                while (names.hasNext()) {
                    String name = names.next();

                    String id;
                    if (values.length() == 1 && name.equals("value")) {
                        id = pointName;
                    } else {
                        id = pointName + "_" + name;
                    }

                    DGMobileContext.CONTEXT.rootNode.getChild(id).update(values.get(name));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
