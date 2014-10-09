package com.dglogik.wear.link;

import android.hardware.Sensor;

import com.dglogik.api.BasicMetaData;
import com.dglogik.dslink.util.BaseNode;
import com.dglogik.value.DGValue;

public class SensorNode extends BaseNode {
    public SensorNode(Sensor sensor) {
        super(sensor.getName());

        setValueMetaData(BasicMetaData.SIMPLE_INT);
    }

    public void update(float value) {
        setValue(DGValue.make(((Float) value).doubleValue()));
    }
}
