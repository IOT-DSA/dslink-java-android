package com.dglogik.common;

import android.app.Application;
import android.support.multidex.MultiDex;

import android.os.*;
import android.app.*;
import android.content.*;
import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

public class DGApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
