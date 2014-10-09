package com.dglogik.wear;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends Activity {
    public Class<?>[] SENSORS = new Class<?>[]{
            StepCounterSensor.class,
            HeartRateSensor.class
    };

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        for (Class<?> sensorClass : SENSORS) {
            Intent intent = new Intent(this, sensorClass);
            startActivity(intent);
        }
    }
}
