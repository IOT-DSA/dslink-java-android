package com.dglogik.mobile.ui;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import com.dglogik.mobile.DGConstants;

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager preferenceManager = getPreferenceManager();
        PreferenceScreen screen = preferenceManager.createPreferenceScreen(getActivity().getApplicationContext());
        PreferenceCategory nodesGroup = new PreferenceCategory(getActivity().getApplicationContext());
        nodesGroup.setTitle("Nodes");
        for (Preference preference : DGConstants.createNodePreferences(getActivity().getApplicationContext())) {
            nodesGroup.addPreference(preference);
        }
        screen.addPreference(nodesGroup);
        setPreferenceScreen(screen);
    }
}
