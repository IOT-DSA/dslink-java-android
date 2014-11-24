package com.dglogik.mobile.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
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

        findPreference("feature.wear").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                try {
                    PackageManager packageManager = getActivity().getPackageManager();
                    packageManager.getPackageInfo("com.google.android.wearable.app", PackageManager.GET_ACTIVITIES);
                } catch (PackageManager.NameNotFoundException e) {
                    showInstallAndroidWearDialog();
                    return false;
                }
                return true;
            }
        });
    }

    private void showInstallAndroidWearDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());

        dialogBuilder.setTitle("Install Android Wear");

        dialogBuilder.setMessage("You must install Android Wear to use this feature.");

        dialogBuilder.setPositiveButton("Install", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                openPlayStore("com.google.android.wearable.app");
            }
        });

        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        dialogBuilder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                dialogInterface.dismiss();
                openPlayStore("com.google.android.wearable.app");
            }
        });

        dialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                dialogInterface.cancel();
            }
        });

        final AlertDialog dialog = dialogBuilder.create();
        dialog.show();
    }

    private void openPlayStore(String packageName) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
            startActivity(intent);
        } catch (Exception e) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
            startActivity(intent);
        }
    }
}
