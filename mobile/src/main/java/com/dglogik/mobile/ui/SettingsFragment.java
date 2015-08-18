package com.dglogik.mobile.ui;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import com.dglogik.mobile.Constants;
import com.dglogik.mobile.R;
import com.google.android.gms.common.AccountPicker;

public class SettingsFragment extends PreferenceFragment {
    static final int REQUEST_CODE_PICK_ACCOUNT = 1000;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        PreferenceCategory nodesCategory = (PreferenceCategory) findPreference("nodes");

        for (Preference preference : Constants.createNodePreferences(getActivity().getApplicationContext())) {
            nodesCategory.addPreference(preference);
        }

        findPreference("feature.wear").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean val = (boolean) newValue;

                if (!val) {
                    return true;
                }

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

        findPreference("feature.fitness").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean val = (boolean) newValue;

                if (!val) {
                    return true;
                }

                pickUserAccount();
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_CODE_PICK_ACCOUNT) {
            SharedPreferences preferences = getPreferenceManager().getSharedPreferences();
            if (resultCode == Activity.RESULT_OK) {
                preferences.edit().putString("account.name", intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)).apply();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(getActivity(), "You must pick an account to use the Fitness System", Toast.LENGTH_SHORT).show();
                preferences.edit().putBoolean("feature.fitness", false).apply();
            }
        }
    }

    private void pickUserAccount() {
        String[] accountTypes = new String[]{"com.google"};
        Intent intent = AccountPicker.newChooseAccountIntent(null, null,
                accountTypes, false, null, null, null, null);
        startActivityForResult(intent, REQUEST_CODE_PICK_ACCOUNT);
    }
}
