package com.dglogik.mobile.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.dglogik.mobile.LinkService;
import com.dglogik.mobile.R;
import com.dglogik.mobile.Utils;

import java.util.Timer;
import java.util.TimerTask;

public class SettingsActivity extends Activity {

    private Button startButton;
    private Button stopButton;
    private Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        timer = new Timer();
        setContentView(R.layout.settings);

        LinearLayout layout = (LinearLayout) findViewById(R.id.settings);

        startButton = new Button(getApplicationContext());
        stopButton = new Button(getApplicationContext());

        startButton.setGravity(Gravity.CENTER_HORIZONTAL);
        stopButton.setGravity(Gravity.CENTER_HORIZONTAL);

        startButton.setText("Start");
        stopButton.setText("Stop");

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startService(new Intent(getApplicationContext(), LinkService.class));
                stopButton.setEnabled(true);
                startButton.setEnabled(false);
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(new Intent(getApplicationContext(), LinkService.class));
                stopButton.setEnabled(false);
                startButton.setEnabled(true);
            }
        });

        syncButtons();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                syncButtons();
            }
        }, 1000, 2000);

        layout.addView(startButton);
        layout.addView(stopButton);
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
}
