package com.dglogik.mobile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.media.ImageReader;
import android.util.DisplayMetrics;
import android.util.Size;
import android.view.Surface;

import org.dsa.iot.dslink.util.handler.Handler;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ResourceType")
public class CameraSupport {
    private DSContext context;

    public CameraSupport(DSContext ctx) {
        this.context = ctx;
    }

    @SuppressWarnings("ConstantConditions")
    public void takePicture(CameraDirection direction, final Handler<byte[]> handler) {
        CameraManager manager = (CameraManager) context.getApplicationContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] ids = manager.getCameraIdList();
            String cameraId = null;
            CameraCharacteristics characteristics = null;
            for (String id : ids) {
                characteristics = manager.getCameraCharacteristics(id);
                int face = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (direction.direction == face) {
                    cameraId = id;
                    break;
                }
            }

            if (cameraId == null) {
                throw new RuntimeException("Camera not found.");
            }

            Size size = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
            final ImageReader reader = ImageReader.newInstance(size.getWidth(), size.getHeight(), ImageFormat.JPEG, 1);
            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    List<Surface> outputs = new ArrayList<>(1);
                    outputs.set(0, reader.getSurface());
                    try {
                        camera.createCaptureSession(outputs, new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(CameraCaptureSession session) {
                                Image img = reader.acquireNextImage();
                                Image.Plane[] planes = img.getPlanes();
                                if (planes[0].getBuffer() == null) {
                                    return;
                                }
                                int width = img.getWidth();
                                int height = img.getHeight();
                                int pixelStride = planes[0].getPixelStride();
                                int rowStride = planes[0].getRowStride();
                                int rowPadding = rowStride - pixelStride * width;
                                byte[] newData = new byte[width * height * 4];

                                int offset = 0;
                                Bitmap bitmap = Bitmap.createBitmap(new DisplayMetrics(), width, height, Bitmap.Config.ARGB_8888);
                                ByteBuffer buffer = planes[0].getBuffer();
                                for (int i = 0; i < height; ++i) {
                                    for (int j = 0; j < width; ++j) {
                                        int pixel = 0;
                                        pixel |= (buffer.get(offset) & 0xff) << 16;     // R
                                        pixel |= (buffer.get(offset + 1) & 0xff) << 8;  // G
                                        pixel |= (buffer.get(offset + 2) & 0xff);       // B
                                        pixel |= (buffer.get(offset + 3) & 0xff) << 24; // A
                                        bitmap.setPixel(j, i, pixel);
                                        offset += pixelStride;
                                    }
                                    offset += rowPadding;
                                }
                                handler.handle(newData);
                                reader.close();
                                session.close();
                            }

                            @Override
                            public void onConfigureFailed(CameraCaptureSession session) {
                                handler.handle(new byte[0]);
                            }
                        }, context.handler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onDisconnected(CameraDevice camera) {
                }

                @Override
                public void onError(CameraDevice camera, int error) {
                    handler.handle(new byte[0]);
                }
            }, context.handler);
        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
