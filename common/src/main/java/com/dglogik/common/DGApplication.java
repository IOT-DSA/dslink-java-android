package com.dglogik.common;

import android.app.Application;
import android.support.multidex.MultiDex;

import android.os.*;
import android.app.*;
import android.content.*;
import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

@ReportsCrashes(
        formKey = "",
        mode = ReportingInteractionMode.DIALOG,
        mailTo = "k.endfinger@dglogik.com"
)
public class DGApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // The following line triggers the initialization of ACRA
        ACRA.init(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
