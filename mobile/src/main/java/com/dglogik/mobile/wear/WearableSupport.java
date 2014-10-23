package com.dglogik.mobile.wear;

import android.support.annotation.NonNull;

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

    public WearableSupport(DGMobileContext context) {
        this.context = context;
        this.nodes = new HashMap<String, DataValueNode>();
    }

    public void initialize() {
        Wearable.MessageApi.addListener(context.googleClient, new DGWearMessageListener());

        Wearable.NodeApi.addListener(context.googleClient, new NodeApi.NodeListener() {
            @Override
            public void onPeerConnected(@NonNull Node node) {
                context.log("Node Connected: " + node.getDisplayName() + " (ID: " + node.getId() + ")");

                if (context.rootNode.hasChild(node.getDisplayName())) {
                    context.log("Node Already Found: " + node.getDisplayName() + " (ID: " + node.getId() + ")");
                    return;
                }

                Wearable.MessageApi.sendMessage(context.googleClient, node.getId(), "/wear/init", null);
            }

            @Override
            public void onPeerDisconnected(@NonNull Node node) {
                if (context.rootNode.hasChild(node.getDisplayName())) {
                    context.rootNode.removeChild(node.getDisplayName());
                }
                context.log("Node Disconnected: " + node.getDisplayName() + " (ID: " + node.getId() + ")");
            }
        });
    }
}
