package com.dglogik.mobile;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import org.dsa.iot.dslink.node.Node;
import org.dsa.iot.dslink.node.value.Value;

public class DSNotificationListenerService extends NotificationListenerService {
    @Override
    public void onListenerConnected() {
        super.onListenerConnected();

        count = getActiveNotifications().length;
        updateCount();
    }

    public int count;

    @Override
    public void onNotificationPosted(StatusBarNotification notification) {
        super.onNotificationPosted(notification);

        count++;
        updateCount();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification notification) {
        super.onNotificationRemoved(notification);

        count--;
        updateCount();
    }

    public void updateCount() {
        if (DSContext.CONTEXT == null || DSContext.CONTEXT.currentDeviceNode == null || !DSContext.CONTEXT.enableNode("notifications")) {
            return;
        }

        Node deviceNode = DSContext.CONTEXT.currentDeviceNode;
        Node activeNotificationsNode = deviceNode.getChild("Active_Notifications");

        if (activeNotificationsNode == null) {
            return;
        }

        activeNotificationsNode.setValue(new Value(count));
    }
}
