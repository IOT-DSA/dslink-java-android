package com.dglogik.mobile.link;

import com.dglogik.api.DGNode;
import com.dglogik.dslink.node.base.BaseNode;

public class RootNode<T extends DGNode> extends BaseNode<T> {
    public RootNode(String name) {
        super(name);
    }

    @Override
    public String getName() {
        System.out.println("RootNode.getName()");
        return super.getName();
    }
}
