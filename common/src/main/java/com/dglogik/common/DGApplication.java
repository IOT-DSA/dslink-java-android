package com.dglogik.common;

import android.app.Application;
import android.support.multidex.MultiDex;

import android.os.*;
import android.app.*;
import android.content.*;
import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender;

@ReportsCrashes(
        formUri = "https://collector.tracepot.com/9f2d3e1a"
)
public class DGApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ACRA.init(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
