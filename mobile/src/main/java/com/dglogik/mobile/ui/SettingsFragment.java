package com.dglogik.mobile.ui;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;

import com.dglogik.mobile.DGConstants;
import com.dglogik.mobile.R;

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        PreferenceCategory nodesCategory = (PreferenceCategory) findPreference("nodes");
        for (Preference preference : DGConstants.createNodePreferences(getActivity().getApplicationContext())) {
            nodesCategory.addPreference(preference);
        }
    }
}
