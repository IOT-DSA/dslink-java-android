package com.dglogik.mobile.link;

import com.dglogik.api.DGContext;
import com.dglogik.api.DGMetaData;
import com.dglogik.mobile.Producer;
import com.dglogik.value.DGValue;

public class LazyDataValueNode extends DataValueNode {
    private Producer<DGValue> onCreate;

    public LazyDataValueNode(String name, DGMetaData metaData) {
        super(name, metaData);
    }

    @Override
    public DGValue getValue(DGContext context) {
        return super.getValue(context);
    }
}
