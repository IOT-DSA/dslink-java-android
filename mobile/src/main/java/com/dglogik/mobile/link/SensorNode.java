package com.dglogik.mobile.link;

import android.hardware.Sensor;

import com.dglogik.api.BasicMetaData;
import com.dglogik.dslink.node.ValuePoint;
import com.dglogik.value.DGValue;

public class SensorNode extends ValuePoint {
    public SensorNode(String name) {
        super(name, BasicMetaData.SIMPLE_INT);
    }

    public void update(double value) {
        setValue(DGValue.make(value));
    }
}
