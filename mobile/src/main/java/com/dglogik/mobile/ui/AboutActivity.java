package com.dglogik.mobile.ui;

import android.app.Activity;
import android.os.Bundle;

import com.dglogik.mobile.R;
import com.dglogik.mobile.Utils;

public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("About DGMobile");
        Utils.applyDGTheme(this);
        setContentView(R.layout.about);
    }
}
