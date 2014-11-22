package com.dglogik.mobile.link;

import com.dglogik.api.DGContext;
import com.dglogik.api.DGMetaData;
import com.dglogik.dslink.node.ValuePoint;
import com.dglogik.mobile.Action;
import com.dglogik.mobile.DGMobileContext;
import com.dglogik.value.DGValue;

public class DataValueNode extends ValuePoint {
    public Action initializeValue;
    public DGMobileContext context = DGMobileContext.CONTEXT;

    public DataValueNode(String name, DGMetaData metaData) {
        super(name, metaData);

        setValue(DGValue.make(null));

//        if (metaData == BasicMetaData.SIMPLE_INT) {
//            makeValue(DGValue.make(0));
//        } else if (metaData == BasicMetaData.SIMPLE_BOOL) {
//            makeValue(DGValue.make(false));
//        } else if (metaData == BasicMetaData.SIMPLE_STRING) {
//            makeValue(DGValue.make(""));
//        }
    }

    public void update(Object obj) {
        if (obj instanceof Boolean) {
            update((Boolean) obj);
        } else if (obj instanceof Integer) {
            update((Integer) obj);
        } else if (obj instanceof Double) {
            update((Double) obj);
        } else if (obj instanceof String) {
            update((String) obj);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void makeValue(DGValue value) {
        setValue(value);
    }

    public void update(Boolean value) {
        makeValue(DGValue.make(value));
    }

    public void update(Integer value) {
        makeValue(DGValue.make(value));
    }

    public void update(Double value) {
        makeValue(DGValue.make(value));
    }

    public void update(String value) {
        makeValue(DGValue.make(value));
    }

    @Override
    public DGValue getValue(DGContext context) {
        DGValue value = super.getValue(context);
        if (value == null) {
            DGMobileContext.log("Node Value is null for " + getName());

            if (initializeValue != null) {
                initializeValue.run();
            }
        }
        return value;
    }
}
