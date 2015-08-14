package com.dglogik.wear;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.dglogik.common.Services;
import com.dglogik.mobile.R;

public class ControllerActivity extends Activity {

    private Button startButton;
    private Button stopButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.controller);

        startButton = (Button) findViewById(R.id.start_button);
        stopButton = (Button) findViewById(R.id.stop_button);

        syncButtons();
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
        if (Services.isServiceRunning(getApplicationContext(), LinkService.class)) {
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
        if (!Services.isServiceRunning(this, LinkService.class)) {
            startService(new Intent(getApplicationContext(), LinkService.class));
        }
        Toast.makeText(getApplicationContext(), "Started DSWear Link", Toast.LENGTH_LONG).show();
        syncButtons();
    }

    public void onStopButtonClicked(View view) {
        if (Services.isServiceRunning(this, LinkService.class)) {
            stopService(new Intent(getApplicationContext(), LinkService.class));
        }
        Toast.makeText(getApplicationContext(), "Stopped DSWear Link", Toast.LENGTH_LONG).show();
        syncButtons();
    }
}
