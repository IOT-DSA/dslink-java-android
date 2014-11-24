package com.dglogik.mobile;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

public class AdminReceiver extends DeviceAdminReceiver {
    @Override
    public void onReceive(Context context, @NonNull Intent intent) {

    }

    @Override
    public void onEnabled(Context context, Intent intent) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putBoolean("device.admin", true).apply();
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putBoolean("device.admin", false).apply();
    }
}
