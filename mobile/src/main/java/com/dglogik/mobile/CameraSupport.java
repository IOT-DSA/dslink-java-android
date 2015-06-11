package com.dglogik.mobile;

import android.hardware.Camera;

import org.vertx.java.core.Handler;

import java.io.IOException;

@SuppressWarnings({"deprecation"})
public class CameraSupport {
    public DGMobileContext context;

    public CameraSupport(DGMobileContext context) {
        this.context = context;
    }

    public void captureImage(CameraDirection direction, final Handler<CameraResult> handler) {
        int count = Camera.getNumberOfCameras();

        if (count == 0) {
            throw new RuntimeException("No Camera");
        }

        Camera camera = null;

        for (int i = 0; i < count; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);

            if (info.facing == direction.direction) {
                camera = Camera.open(i);
                break;
            }
        }

        if (camera == null) {
            camera = Camera.open(0);
        }

        try {
            camera.setPreviewDisplay(null);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }

        camera.startPreview();
        camera.takePicture(new Camera.ShutterCallback() {
            @Override
            public void onShutter() {
            }
        }, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                handler.handle(new CameraResult(data));
                camera.release();
            }
        });
    }
}
