package com.dglogik.mobile;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        DGMobileContext context = new DGMobileContext(this);

        context.onCreate();
    }
}
