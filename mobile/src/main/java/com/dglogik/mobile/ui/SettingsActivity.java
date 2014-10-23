package com.dglogik.mobile.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.dglogik.mobile.LinkService;
import com.dglogik.mobile.R;
import com.dglogik.mobile.Utils;

import java.util.Timer;
import java.util.TimerTask;

public class SettingsActivity extends Activity {

    private Button startButton;
    private Button stopButton;
    private Timer timer;
    private TimerTask syncButtonsTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        timer = new Timer();
        setContentView(R.layout.settings);

        startButton = (Button) findViewById(R.id.start_button);
        stopButton = (Button) findViewById(R.id.stop_button);

        syncButtons();

        syncButtonsTask = new TimerTask() {
            @Override
            public void run() {
                syncButtons();
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();

        timer.scheduleAtFixedRate(syncButtonsTask, 1000, 2000);
    }

    @Override
    public void onPause() {
        super.onPause();

        syncButtonsTask.cancel();
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
        timer.cancel();
    }

    public void onStartButtonClicked(View view) {
        startService(new Intent(getApplicationContext(), LinkService.class));
        Toast.makeText(getApplicationContext(), "Started DGMobile Link", Toast.LENGTH_LONG).show();
        stopButton.setEnabled(true);
        startButton.setEnabled(false);
    }

    public void onStopButtonClicked(View view) {
        stopService(new Intent(getApplicationContext(), LinkService.class));
        Toast.makeText(getApplicationContext(), "Stopped DGMobile Link", Toast.LENGTH_LONG).show();
        stopButton.setEnabled(false);
        startButton.setEnabled(true);
    }
}
