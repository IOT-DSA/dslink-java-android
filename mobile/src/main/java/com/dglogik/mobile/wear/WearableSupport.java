package com.dglogik.mobile.wear;

import android.support.annotation.NonNull;

import com.dglogik.api.DGNode;
import com.dglogik.mobile.Action;
import com.dglogik.mobile.DGMobileContext;
import com.dglogik.mobile.link.DataValueNode;
import com.dglogik.mobile.link.DeviceNode;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class WearableSupport {
    public final DGMobileContext context;
    @NonNull
    public final Map<String, DataValueNode> nodes;
    public final Map<String, String> namesMap = new HashMap<>();

    public WearableSupport(DGMobileContext context) {
        this.context = context;
        this.nodes = new HashMap<>();
    }

    public void initialize() {
        final DGWearMessageListener messageListener = new DGWearMessageListener();
        Wearable.MessageApi.addListener(context.googleClient, messageListener);

        context.onCleanup(new Action() {
            @Override
            public void run() {
                Wearable.MessageApi.removeListener(context.googleClient, messageListener);
            }
        });

        context.poller(new Action() {
            @Override
            public void run() {
                Wearable.NodeApi.getConnectedNodes(context.googleClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                    @Override
                    public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                        for (Node node : getConnectedNodesResult.getNodes()) {
                            if (!wearNodes.contains(node.getId())) {
                                Wearable.MessageApi.sendMessage(context.googleClient, node.getId(), "/wear/init", null);
                            }
                        }
                    }
                });
            }
        }).poll(TimeUnit.SECONDS, 4, false);
    }

    public Set<String> wearNodes = new HashSet<>();
}
