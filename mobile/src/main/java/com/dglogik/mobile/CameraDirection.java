package com.dglogik.mobile;

import android.hardware.Camera;

@SuppressWarnings({"deprecation"})
public enum CameraDirection {
    BACK(Camera.CameraInfo.CAMERA_FACING_BACK),
    FRONT(Camera.CameraInfo.CAMERA_FACING_FRONT);

    public final int direction;

    CameraDirection(int direction) {
        this.direction = direction;
    }
}
