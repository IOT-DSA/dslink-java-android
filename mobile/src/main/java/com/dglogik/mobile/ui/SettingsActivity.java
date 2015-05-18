package com.dglogik.mobile.ui;

import android.app.Activity;
import android.os.Bundle;

import com.dglogik.mobile.Utils;

public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("DSAndroid Settings");
        Utils.applyDGTheme(this);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }
}
