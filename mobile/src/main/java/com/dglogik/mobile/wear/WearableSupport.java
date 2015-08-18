package com.dglogik.mobile.wear;

import android.support.annotation.NonNull;

import com.dglogik.mobile.DSContext;
import com.dglogik.mobile.Executable;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class WearableSupport {
    public final DSContext context;
    @NonNull
    public final Map<String, Node> nodes;
    public final Map<String, String> namesMap = new HashMap<>();

    public WearableSupport(DSContext context) {
        this.context = context;
        this.nodes = new HashMap<>();
    }

    public void initialize() {
        final WearMessageListener messageListener = new WearMessageListener();
        Wearable.MessageApi.addListener(context.googleClient, messageListener);

        context.onCleanup(new Executable() {
            @Override
            public void run() {
                Wearable.MessageApi.removeListener(context.googleClient, messageListener);
            }
        });

        context.poller(new Executable() {
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