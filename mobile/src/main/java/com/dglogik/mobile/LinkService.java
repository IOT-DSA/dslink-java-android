package com.dglogik.mobile;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class LinkService extends Service {
    public DGMobileContext context;
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        context = new DGMobileContext(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        context.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        context.destroy();
    }
}
