package com.dglogik.mobile;

public class CameraResult {
    private final byte[] data;

    public CameraResult(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }
}
