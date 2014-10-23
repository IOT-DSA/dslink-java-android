package com.dglogik.mobile;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.support.annotation.NonNull;

public class Utils {
    public static boolean isServiceRunning(@NonNull Context context, @NonNull Class<? extends Service> clazz) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (service.service.getClassName().equals(clazz.getName())) {
                return true;
            }
        }

        return false;
    }

    @NonNull
    public static Thread startThread(Runnable action) {
        Thread thread = new Thread(action);
        thread.start();
        return thread;
    }
}
