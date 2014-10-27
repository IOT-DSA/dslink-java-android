package com.dglogik.mobile.wear;

import android.support.annotation.NonNull;

import com.dglogik.mobile.Action;
import com.dglogik.mobile.DGMobileContext;
import com.dglogik.mobile.link.DataValueNode;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.HashMap;
import java.util.Map;

public class WearableSupport {
    public final DGMobileContext context;
    @NonNull
    public final Map<String, DataValueNode> nodes;
    public Map<String, String> namesMap = new HashMap<String, String>();

    public WearableSupport(DGMobileContext context) {
        this.context = context;
        this.nodes = new HashMap<String, DataValueNode>();
    }

    public void initialize() {
        final DGWearMessageListener messageListener = new DGWearMessageListener();
        Wearable.MessageApi.addListener(context.googleClient, messageListener);

        final NodeApi.NodeListener nodeListener = new NodeApi.NodeListener() {
            @Override
            public void onPeerConnected(@NonNull Node node) {
                context.log("Node Connected: " + node.getDisplayName() + " (ID: " + node.getId() + ")");

                String deviceName = namesMap.get(node.getId());

                if (deviceName != null && context.rootNode.hasChild(deviceName)) {
                    context.log("Node Already Found: " + node.getDisplayName() + " (ID: " + node.getId() + ")");
                    return;
                }

                Wearable.MessageApi.sendMessage(context.googleClient, node.getId(), "/wear/init", null);
            }

            @Override
            public void onPeerDisconnected(@NonNull Node node) {
                String deviceName = namesMap.get(node.getId());
                if (deviceName != null && context.rootNode.hasChild(deviceName)) {
                    context.rootNode.removeChild(deviceName);
                }
                context.log("Node Disconnected: " + node.getDisplayName() + " (ID: " + node.getId() + ")");
            }
        };

        Wearable.NodeApi.addListener(context.googleClient, nodeListener);

        context.onCleanup(new Action() {
            @Override
            public void run() {
                Wearable.NodeApi.removeListener(context.googleClient, nodeListener);
                Wearable.MessageApi.removeListener(context.googleClient, messageListener);
            }
        });
    }
}
