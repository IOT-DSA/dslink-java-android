package com.dglogik.mobile.link;

import com.dglogik.api.BasicMetaData;
import com.dglogik.api.DGMetaData;
import com.dglogik.dslink.node.ValuePoint;
import com.dglogik.value.DGValue;

public class DataValueNode extends ValuePoint {
    public DataValueNode(String name, DGMetaData metaData) {
        super(name, metaData);

        if (metaData == BasicMetaData.SIMPLE_INT) {
            makeValue(DGValue.make(0));
        } else if (metaData == BasicMetaData.SIMPLE_BOOL) {
            makeValue(DGValue.make(false));
        } else if (metaData == BasicMetaData.SIMPLE_STRING) {
            makeValue(DGValue.make(""));
        }
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
        System.out.println("Updating Value for "  + getName() + " to " + value);
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
}
