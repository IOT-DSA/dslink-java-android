package com.dglogik.mobile;

import android.util.Base64;

public class CameraResult {
    private final byte[] data;

    public CameraResult(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public String encodeDataToBase64() {
        return Base64.encodeToString(data, Base64.DEFAULT);
    }
}
