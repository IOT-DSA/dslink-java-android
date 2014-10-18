package com.dglogik.mobile.wear;

import android.content.Intent;

import com.dglogik.mobile.DGMobileContext;
import com.dglogik.mobile.link.SensorNode;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.eclipse.jetty.util.MultiMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WearableSupport {
    public final DGMobileContext context;
    public final Map<String, SensorNode> nodes;
    public final List<String> sensorNames = new ArrayList<String>();

    public WearableSupport(DGMobileContext context) {
        this.context = context;
        this.nodes = new HashMap<String, SensorNode>();
    }

    public void initialize() {
        Wearable.MessageApi.addListener(context.googleClient, new DGWearMessageListener());
    }
}
