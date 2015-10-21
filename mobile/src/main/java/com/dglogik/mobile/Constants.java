package com.dglogik.mobile;

import android.content.Context;
import android.graphics.Color;
import android.preference.Preference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Constants {
    public static final String START_ON_BOOT = "start.on.boot";

    public static final int BRAND_COLOR = Color.parseColor("#4dd0e1");

    @SuppressWarnings("RedundantArrayCreation")
    public static final List<NodeDescriptor> NODES = Arrays.asList(new NodeDescriptor[] {
            new NodeDescriptor("Screen Status", "screen"),
            new NodeDescriptor("Location Information", "location"),
            new NodeDescriptor("Battery Information", "battery"),
            new NodeDescriptor("Step Counter", "steps"),
            new NodeDescriptor("Temperature", "temperature"),
            new NodeDescriptor("Light Level", "light_level"),
            new NodeDescriptor("Air Pressure", "pressure"),
            new NodeDescriptor("Humidity", "humidity"),
            new NodeDescriptor("Rotation", "rotation"),
            new NodeDescriptor("Gyroscope", "gyroscope"),
            new NodeDescriptor("Proximity", "proximity"),
//            new NodeDescriptor("Audio", "audio"),
            new NodeDescriptor("Music", "music"),
            new NodeDescriptor("Activity Recognition", "activity"),
            new NodeDescriptor("Heart Rate", "heart_rate"),
            new NodeDescriptor("Notifications", "notifications"),
            new NodeDescriptor("Speech", "speech"),
            new NodeDescriptor("Current Application", "current_app"),
            new NodeDescriptor("Camera", "camera"),
            new NodeDescriptor("Gravity", "gravity")
    });

    public static List<Preference> createNodePreferences(Context context) {
        List<Preference> preferences = new ArrayList<>();
        for (NodeDescriptor descriptor : Constants.NODES) {
            Preference preference = descriptor.createCheckboxPreference(context);
            preferences.add(preference);
        }
        return preferences;
    }
}
