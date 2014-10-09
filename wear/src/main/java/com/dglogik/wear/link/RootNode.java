package com.dglogik.wear.link;

import com.dglogik.api.BasicMetaData;
import com.dglogik.api.DGAction;
import com.dglogik.api.DGContext;
import com.dglogik.api.DGMetaData;
import com.dglogik.dslink.node.Role;
import com.dglogik.dslink.util.BaseNode;
import com.dglogik.util.MessageException;
import com.dglogik.value.DGValue;
import com.dglogik.wear.SensorRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * Root node for the node API
 */
public class RootNode extends BaseNode {

    public RootNode() {
        super("Android Wear");
        addActions();
    }

    private void addActions() {
        addAction(new DGAction() {
            @Override
            public String getName() {
                return "addSensorListener";
            }

            @Override
            public Map<String, DGMetaData> getParameters(DGContext cx) {
                if (!Role.get(cx).hasPermission(Role.VIEW))
                    return null;
                Map<String, DGMetaData> params = new HashMap<String, DGMetaData>();
                params.put("sensor", BasicMetaData.SIMPLE_STRING);
                return params;
            }

            @Override
            public Map<String, DGMetaData> getResults(DGContext cx) {
                if (!Role.get(cx).hasPermission(Role.VIEW))
                    return null;
                return null;
            }
        });
    }

    @Override
    public Map<String, DGValue> invoke(
            String action, Map<String, DGValue> parameters, DGContext cx) {
        if (!Role.get(cx).hasPermission(Role.USER))
            return null;

        if (action.equals("addSensorListener")) {
            DGValue sensorName = parameters.get("sensor");

            if (!SensorRegistry.hasSensor(sensorName.toString())) {
                throw new MessageException("No Such Sensor");
            }

            SensorNode node = new SensorNode(SensorRegistry.getActivity(sensorName.toString()).sensor);

            SensorRegistry.applyNode(sensorName.toString(), node);
            addChild(node);
        } else {
            throw new MessageException("Unknown action");
        }
        return null;
    }
}