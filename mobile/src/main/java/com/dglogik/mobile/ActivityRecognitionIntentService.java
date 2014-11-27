package com.dglogik.mobile;

import android.app.IntentService;
import android.content.Intent;
import android.app.*;
import android.util.Log;
import com.google.android.gms.location.*;

public class ActivityRecognitionIntentService extends IntentService {
    public ActivityRecognitionIntentService() {
        super("ActivityRecognitionIntent");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        DGMobileContext.log("Got Activity Recognition Event");
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            if (DGMobileContext.CONTEXT.activityNode != null) {
                String name = getNameFromType(result.getMostProbableActivity().getType());
                DGMobileContext.CONTEXT.activityNode.update(name);
            }
        }
    }

    private String getNameFromType(int activityType) {
        switch(activityType) {
            case DetectedActivity.IN_VEHICLE:
                return "in_vehicle";
            case DetectedActivity.ON_BICYCLE:
                return "on_bicycle";
            case DetectedActivity.ON_FOOT:
                return "on_foot";
            case DetectedActivity.STILL:
                return "still";
            case DetectedActivity.UNKNOWN:
                return "unknown";
            case DetectedActivity.TILTING:
                return "tilting";
        }
        return "unknown";
    }
}
