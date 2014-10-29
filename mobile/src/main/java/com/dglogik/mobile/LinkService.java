package com.dglogik.mobile;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.dglogik.mobile.ui.ControllerActivity;

import org.jetbrains.annotations.Nullable;

public class LinkService extends Service {
    public DGMobileContext context;
    public Notification notification;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        context = new DGMobileContext(this);
        Notification.Builder builder = new Notification.Builder(getApplicationContext());
        builder.setContentTitle("DGMobile Link");
        builder.setContentText("Link is Running");
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 1, new Intent(getApplicationContext(), ControllerActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        notification = builder.build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(0, notification);
        context.start();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        DGMobileContext.log("Destroying Context");
        context.destroy();
    }

    @Override
    public void onTrimMemory(int level) {
        boolean stopOnLowMemory = context.preferences.getBoolean("stop.on.low.memory", false);

        if ((level == TRIM_MEMORY_RUNNING_LOW || level == TRIM_MEMORY_RUNNING_CRITICAL) && stopOnLowMemory) {
            stopSelf();
        }
    }
}
