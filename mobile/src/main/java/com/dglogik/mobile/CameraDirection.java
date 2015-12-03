package com.dglogik.mobile;

import android.hardware.camera2.CameraCharacteristics;

@SuppressWarnings({"deprecation"})
public enum CameraDirection {
    BACK(CameraCharacteristics.LENS_FACING_BACK),
    FRONT(CameraCharacteristics.LENS_FACING_FRONT);

    public final int direction;

    CameraDirection(int direction) {
        this.direction = direction;
    }
}
