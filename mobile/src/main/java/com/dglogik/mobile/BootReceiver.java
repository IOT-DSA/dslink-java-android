package com.dglogik.mobile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.dglogik.common.Services;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(@NonNull Context context, Intent intent) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

        if (settings.getBoolean(Constants.START_ON_BOOT, false) && !Services.isServiceRunning(context, LinkService.class)) {
            Intent linkIntent = new Intent(context, LinkService.class);
            context.startService(linkIntent);
        }
    }
}
