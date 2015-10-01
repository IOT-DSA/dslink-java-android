package com.dglogik.mobile;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.value.Value;

public class DSAccessibilityService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (DSContext.CONTEXT == null || DSContext.CONTEXT.currentDeviceNode == null || !DSContext.CONTEXT.enableNode("current_app")) {
            return;
        }

        Node deviceNode = DSContext.CONTEXT.currentDeviceNode;
        Node currentApplicationNode = deviceNode.getChild("Current_Application");
        if (currentApplicationNode != null && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String packageName = event.getPackageName().toString();
            currentApplicationNode.setValue(new Value(packageName));
        }
    }

    @Override
    public void onInterrupt() {
    }
}
