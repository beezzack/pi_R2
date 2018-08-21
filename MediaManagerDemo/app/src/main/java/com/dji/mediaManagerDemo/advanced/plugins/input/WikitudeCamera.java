package com.dji.mediaManagerDemo.advanced.plugins.input;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;

import java.io.IOException;

/**
 * Minimal implementation of the camera api.
 */
public class WikitudeCamera implements Camera.ErrorCallback {

    private static final String TAG = "WikitudeCamera";
    private int frameWidth;
    private int frameHeight;
    private double fieldOfView;
    private Camera camera;
    private Camera.Parameters cameraParameters;
    private Object texture;

    public WikitudeCamera(int frameWidth, int frameHeight) {
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;

    }

    public void start(Camera.PreviewCallback previewCallback) {
        try {
            camera = Camera.open(getCamera());
            camera.setErrorCallback(this);
            camera.setPreviewCallback(previewCallback);
            cameraParameters = camera.getParameters();
            cameraParameters.setPreviewFormat(ImageFormat.NV21);
            Camera.Size cameraSize = getCameraSize(frameWidth, frameHeight);
            cameraParameters.setPreviewSize(cameraSize.width, cameraSize.height);
            fieldOfView = cameraParameters.getHorizontalViewAngle();
            camera.setParameters(cameraParameters);
            texture = new SurfaceTexture(0);
            camera.setPreviewTexture((SurfaceTexture) texture);
            camera.startPreview();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (RuntimeException ex) {
            Log.e(TAG, "Camera not found: " + ex);
        }
    }

    public void close() {
        try {
            if (camera != null) {
                camera.setPreviewCallback(null);
                camera.stopPreview();
                camera.release();
                camera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(int error, Camera camera) {
        if (this.camera != null) {
            this.camera.release();
            this.camera = null;
        }
    }

    private Camera.Size getCameraSize(int desiredWidth, int desiredHeight) {
        for (Camera.Size size : cameraParameters.getSupportedPreviewSizes()) {
            if (size.width == desiredWidth && size.height == desiredHeight) {
                return size;
            }
        }
        return cameraParameters.getSupportedPreviewSizes().get(0);
    }

    private int getCamera() {
        try {
            int numberOfCameras = Camera.getNumberOfCameras();
            final Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

            for (int cameraId = 0; cameraId < numberOfCameras; cameraId++) {
                Camera.getCameraInfo(cameraId, cameraInfo);

                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    return cameraId;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public int getImageSensorRotation() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

        int cameraId = getCamera();

        if (cameraId != -1) {
            Camera.getCameraInfo(cameraId, cameraInfo);
            int imageSensorRotation = cameraInfo.orientation;
            return 360 - imageSensorRotation; // the android API returns CW values (WHY?), 360 - X to have CCW
        } else {
            throw new RuntimeException("The getCamera function failed to return a valid camera ID. The image sensor rotation could therefore not be evaluated.");
        }
    }

    public double getCameraFieldOfView() {
        return fieldOfView;
    }

    public int getFrameWidth() {
        return frameWidth;
    }

    public int getFrameHeight() {
        return frameHeight;
    }


}