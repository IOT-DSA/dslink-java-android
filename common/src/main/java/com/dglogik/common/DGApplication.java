package com.dglogik.common;

import android.app.Application;
import org.acra.*;
import org.acra.annotation.*;

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
}
