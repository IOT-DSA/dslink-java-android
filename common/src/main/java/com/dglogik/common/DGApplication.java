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
        httpMethod = HttpSender.Method.PUT,
        reportType = HttpSender.Type.JSON,
        formUri = "http://directcode.org:5984/acra-dsandroid/_design/acra-storage/_update/report",
        formUriBasicAuthLogin = "reporter",
        formUriBasicAuthPassword = "reporter"
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
