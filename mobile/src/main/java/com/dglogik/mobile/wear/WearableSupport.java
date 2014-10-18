package com.dglogik.mobile.wear;

import com.dglogik.mobile.DGMobileContext;
import com.dglogik.mobile.link.DataValueNode;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.HashMap;
import java.util.Map;

public class WearableSupport {
    public final DGMobileContext context;
    public final Map<String, DataValueNode> nodes;

    public WearableSupport(DGMobileContext context) {
        this.context = context;
        this.nodes = new HashMap<String, DataValueNode>();
    }

    public void initialize() {
        Wearable.MessageApi.addListener(context.googleClient, new DGWearMessageListener());

        Wearable.NodeApi.addListener(context.googleClient, new NodeApi.NodeListener() {
            @Override
            public void onPeerConnected(Node node) {
                System.out.println("Node Connected: " + node.getDisplayName() + " (ID: " + node.getId() + ")");
                Wearable.MessageApi.sendMessage(context.googleClient, node.getId(), "/wear/init", null);
            }

            @Override
            public void onPeerDisconnected(Node node) {
                System.out.println("Node Disconnected: " + node.getDisplayName() + " (ID: " + node.getId() + ")");
            }
        });
    }
}
