package com.dglogik.mobile.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.dglogik.mobile.LinkService;
import com.dglogik.mobile.R;
import com.dglogik.mobile.Utils;

import java.util.Timer;
import java.util.TimerTask;

public class ControllerActivity extends Activity {

    private Button startButton;
    private Button stopButton;

    @SuppressWarnings("FieldCanBeLocal")
    private Timer timer;
    @SuppressWarnings("FieldCanBeLocal")
    private TimerTask syncTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.controller);

        timer = new Timer();

        startButton = (Button) findViewById(R.id.start_button);
        stopButton = (Button) findViewById(R.id.stop_button);

        syncButtons();

        syncTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        syncButtons();
                    }
                });
            }
        };

        timer.scheduleAtFixedRate(syncTask, 0, 2000);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.controller_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                openSettings();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void openSettings() {
        Intent intent = new Intent();
        intent.setClass(getApplicationContext(), SettingsActivity.class);
        startActivityForResult(intent, 50);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 50) { // Settings Activity
            onSettingsClosed();
        }
    }

    private void onSettingsClosed() {
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void syncButtons() {
        if (Utils.isServiceRunning(getApplicationContext(), LinkService.class)) {
            stopButton.setEnabled(true);
            startButton.setEnabled(false);
        } else {
            stopButton.setEnabled(false);
            startButton.setEnabled(true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void onStartButtonClicked(View view) {
        if (!Utils.isServiceRunning(getApplicationContext(), LinkService.class)) {
            startService(new Intent(getApplicationContext(), LinkService.class));
            Toast.makeText(getApplicationContext(), "Started DGMobile Link", Toast.LENGTH_LONG).show();
        }
        syncButtons();
    }

    public void onStopButtonClicked(View view) {
        if (Utils.isServiceRunning(getApplicationContext(), LinkService.class)) {
            stopService(new Intent(getApplicationContext(), LinkService.class));
            Toast.makeText(getApplicationContext(), "Stopped DGMobile Link", Toast.LENGTH_LONG).show();
        }
        syncButtons();
    }
}
