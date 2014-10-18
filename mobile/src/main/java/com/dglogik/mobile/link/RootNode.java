package com.dglogik.mobile.link;

import com.dglogik.dslink.node.base.BaseNode;
import com.dglogik.mobile.DGMobileContext;

/**
 * Root node for the node API
 */
public class RootNode extends BaseNode<SensorNode> {

    public RootNode() {
        super("Watch");
        addChildren();
    }

    private void addChildren() {
        for (String name : DGMobileContext.CONTEXT.wearable.sensorNames) {
            System.out.println(name);
            SensorNode node = new SensorNode(name);
            DGMobileContext.CONTEXT.wearable.nodes.put(name, node);
            addChild(node);
        }
    }
}