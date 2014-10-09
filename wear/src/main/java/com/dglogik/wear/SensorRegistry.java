package com.dglogik.wear;

import com.dglogik.wear.link.SensorNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SensorRegistry {
    protected static Map<String, SensorActivity> sensors = new HashMap<String, SensorActivity>();
    protected static Map<String, List<SensorNode>> nodes = new HashMap<String, List<SensorNode>>();

    public static void register(String name, SensorActivity activity) {
        sensors.put(name, activity);
    }

    public static boolean hasSensor(String name) {
        return sensors.containsKey(name);
    }

    public static SensorActivity getActivity(String name) {
        return sensors.get(name);
    }

    public static void applyNode(String name, SensorNode node) {
        if (!nodes.containsKey(name)) {
            nodes.put(name, new ArrayList<SensorNode>());
        }

        nodes.get(name).add(node);
    }

    public static void emit(String name, float value) {
        if (nodes.containsKey(name)) {
            List<SensorNode> all = nodes.get(name);

            for (SensorNode node : all) {
                node.update(value);
            }
        }
    }
}
