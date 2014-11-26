package com.dglogik.mobile.link;

import com.dglogik.dslink.node.base.BaseNode;

/**
 * Root node for the node API
 */
public class DeviceNode extends BaseNode<BaseNode> {

    public DeviceNode(String name) {
        super(name);
    }

    @Override
    public void addChild(BaseNode node) {
        if (node.getName().contains("_")) {
            node.setDisplayName(node.getName().replaceAll("_", " "));
        }
        super.addChild(node);
    }
}
