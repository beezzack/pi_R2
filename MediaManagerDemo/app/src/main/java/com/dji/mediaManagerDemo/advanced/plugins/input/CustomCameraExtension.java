package com.dji.mediaManagerDemo.advanced.plugins.input;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;


import com.dji.mediaManagerDemo.R;
import com.dji.mediaManagerDemo.VideoDecodingApplication;
import com.dji.mediaManagerDemo.media.DJIVideoStreamDecoder;
import com.dji.mediaManagerDemo.media.NativeHelper;
import com.wikitude.architect.ArchitectView;
import com.wikitude.common.plugins.PluginManager;
import com.dji.mediaManagerDemo.advanced.ArchitectViewExtension;
import java.nio.ByteBuffer;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.product.Model;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;

/**
 * This Activity is the java counterpart of the 13_PluginsAPI_4_CustomCamera AR-Experience.
 * It registers a native Plugin which uses camera frames from a custom camera implementation
 * and will render the camera frame and the augmentations in the Plugin Code.
 *
 * Please Note that the custom camera implementations are very minimal and a more advanced Camera implementation
 * should be used in apps.
 */
public class CustomCameraExtension extends ArchitectViewExtension implements DJICodecManager.YuvDataCallback{
    protected VideoFeeder.VideoDataCallback mReceivedVideoDataCallBack = null;
    private Camera mCamera;
    private static final String TAG = "CustomCamera";
    private DJIVideoStreamDecoder decoder;
    private int count;
    private int width = 1024;
    private int height = 768;
    private double FOV = 81.9;
    private boolean flag = false;
    Context context;

    public CustomCameraExtension(Activity activity, ArchitectView architectView) {
        super(activity, architectView);
    }

    @Override
    public void onPostCreate() {
        /*
         * Registers the plugin with the name "customcamera".
         * The library containing the native plugin is libwikitudePlugins.so.
         */
        architectView.registerNativePlugins("wikitudePlugins", "customcamera", new PluginManager.PluginErrorCallback() {
            @Override
            public void onRegisterError(int errorCode, String errorMessage) {
                Toast.makeText(activity, R.string.error_loading_plugins, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Plugin failed to load. Reason: " + errorMessage);
            }
        });
        initNative();
    }

    /**
     * This is called via JNI from c++.
     * It will create the custom Camera based on OS version(old camera/camera2).
     */
    public void onInputPluginInitialized() {
        Log.v(TAG, "onInputPluginInitialized");
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setFrameSize(width, height);
                setCameraFieldOfView(FOV);
                setImageSensorRotation(270);
                setDefaultDeviceOrientationLandscape(true);
            }
        });
    }
    /**
     * This is called via JNI from c++.
     * Will close the camera preview.
     */
    public void onInputPluginPaused() {
        Log.v(TAG, "onInputPluginPaused");

    }
    /**
     * This is called via JNI from c++.
     * Will start the camera preview.
     */
    public void onInputPluginResumed() {
        Log.v(TAG, "onInputPluginResumed");

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyStatusChange();
            }
        });
    }

    /**
     * This is called via JNI from c++.
     * Will close the camera preview.
     */
    public void onInputPluginDestroyed() {
        Log.v(TAG, "onInputPluginDestroyed");

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    @Override
    public void onYuvDataReceived(final ByteBuffer yuvFrame, int dataSize, final int width, final int height) {
        //In this demo, we test the YUV data by saving it into JPG files.
        //DJILog.d(TAG, "onYuvDataReceived " + dataSize);

        final byte[] bytes = new byte[dataSize];
        yuvFrame.get(bytes);
//            DJILog.d(TAG, "onYuvDataReceived2 " + dataSize);
        Log.d(TAG,"datasize:" + dataSize + "\nwidth:" + width + "\nheight:" + height );
        saveYuvDataToJPEG(bytes, width, height);

    }
    private void saveYuvDataToJPEG(byte[] yuvFrame, int width, int height){
        if (yuvFrame.length < width * height) {
            //DJILog.d(TAG, "yuvFrame size is too small " + yuvFrame.length);
            return;
        }

        byte[] y = new byte[width * height];
        byte[] u = new byte[width * height / 4];
        byte[] v = new byte[width * height / 4];
        byte[] nu = new byte[width * height / 4]; //
        byte[] nv = new byte[width * height / 4];

        System.arraycopy(yuvFrame, 0, y, 0, y.length);
        for (int i = 0; i < u.length; i++) {
            v[i] = yuvFrame[y.length + u.length + i];
            u[i] = yuvFrame[y.length + i];
        }
//        int uvWidth = width / 2;
//        int uvHeight = height / 2;
//        for (int j = 0; j < uvWidth / 2; j++) {
//            for (int i = 0; i < uvHeight / 2; i++) {
//                byte uSample1 = u[i * uvWidth + j];
//                byte uSample2 = u[i * uvWidth + j + uvWidth / 2];
//                byte vSample1 = v[(i + uvHeight / 2) * uvWidth + j];
//                byte vSample2 = v[(i + uvHeight / 2) * uvWidth + j + uvWidth / 2];
//                nu[2 * (i * uvWidth + j)] = uSample1;
//                nu[2 * (i * uvWidth + j) + 1] = uSample1;
//                nu[2 * (i * uvWidth + j) + uvWidth] = uSample2;
//                nu[2 * (i * uvWidth + j) + 1 + uvWidth] = uSample2;
//                nv[2 * (i * uvWidth + j)] = vSample1;
//                nv[2 * (i * uvWidth + j) + 1] = vSample1;
//                nv[2 * (i * uvWidth + j) + uvWidth] = vSample2;
//                nv[2 * (i * uvWidth + j) + 1 + uvWidth] = vSample2;
//            }
//        }
        //nv21test
        byte[] bytes = new byte[yuvFrame.length];
        System.arraycopy(y, 0, bytes, 0, y.length);
        for (int i = 0; i < u.length; i++) {
            bytes[y.length + (i * 2)] = v[i];
            bytes[y.length + (i * 2) + 1] = u[i];
        }
        Log.d(TAG,"onYuvDataReceived: frame index: "
                        + DJIVideoStreamDecoder.getInstance().frameIndex
                        + ",array length: "
                        + bytes.length);
        notifyNewCameraFrameN21(bytes);
    }

    private void notifyStatusChange() {
        Log.d(TAG, "notifyStatusChange ");
        final BaseProduct product = VideoDecodingApplication.getProductInstance();
        // The callback for receiving the raw H264 video data for camera live view
        mReceivedVideoDataCallBack = new VideoFeeder.VideoDataCallback() {
            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                Log.d(TAG, "camera recv video data size: " + size);
                DJIVideoStreamDecoder.getInstance().parse(videoBuffer, size);
            }
        };
        if (null == product || !product.isConnected()) {
            mCamera = null;
            Log.d(TAG, "Disconnected!");
        } else {
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                mCamera = product.getCamera();
                mCamera.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            Log.d(TAG, "can't change mode of camera, error:"+djiError.getDescription());
                        }
                    }
                });
                if (VideoFeeder.getInstance().getPrimaryVideoFeed() != null) {
                    VideoFeeder.getInstance().getPrimaryVideoFeed().setCallback(mReceivedVideoDataCallBack);
                }
            }
        }
    }



    private native void initNative();
    private native void notifyNewCameraFrameN21(byte[] frameData);
    private native void setCameraFieldOfView(double fieldOfView);
    private native void setFrameSize(int frameWidth, int frameHeight);
    private native void setDefaultDeviceOrientationLandscape(boolean isLandscape);
    private native void setImageSensorRotation(int rotation);
}
