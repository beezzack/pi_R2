package com.dji.mediaManagerDemo.advanced.plugins.input;

import com.wikitude.architect.ArchitectView;
import com.wikitude.common.plugins.PluginManager;
import com.dji.mediaManagerDemo.advanced.ArchitectViewExtension;
import com.dji.mediaManagerDemo.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.hardware.Camera;
import android.media.Image;
import android.media.ImageReader;
import android.util.Log;
import android.widget.Toast;

import java.nio.ByteBuffer;

/**
 * This Activity is the java counterpart of the 13_PluginsAPI_3_SimpleInputPlugin AR-Experience.
 * It registers a native Plugin which uses camera frames from a custom camera implementation
 * and will let the Wikitude SDK render the camera frame and augmentations.
 *
 * Please Note that the custom camera implementations are very minimal and a more advanced Camera implementation
 * should be used in apps.
 */
public class SimpleInputPluginExtension extends ArchitectViewExtension {

    private static final String TAG = "SimpleInputPlugin";

    private static final int FRAME_WIDTH = 640;
    private static final int FRAME_HEIGHT = 480;

    private WikitudeCamera2 wikitudeCamera2;
    private WikitudeCamera wikitudeCamera;

    public SimpleInputPluginExtension(Activity activity, ArchitectView architectView) {
        super(activity, architectView);
    }

    @Override
    public void onPostCreate() {
        /*
         * Registers the plugin with the name "simple_input_plugin".
         * The library containing the native plugin is libwikitudePlugins.so.
         */
        architectView.registerNativePlugins("wikitudePlugins", "simple_input_plugin", new PluginManager.PluginErrorCallback() {
            @Override
            public void onRegisterError(int errorCode, String errorMessage) {
                Toast.makeText(activity, R.string.error_loading_plugins, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Plugin failed to load. Reason: " + errorMessage);
            }
        });
        initNative();
        setFrameSize(FRAME_WIDTH, FRAME_HEIGHT);
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
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                    wikitudeCamera2 = new WikitudeCamera2(activity, FRAME_WIDTH, FRAME_HEIGHT);
                    setCameraFieldOfView((wikitudeCamera2.getCameraFieldOfView()));

                    int imageSensorRotation = wikitudeCamera2.getImageSensorRotation();
                    if (imageSensorRotation != 0) {
                        setImageSensorRotation(imageSensorRotation);
                    }
                } else {
                    wikitudeCamera = new WikitudeCamera(FRAME_WIDTH, FRAME_HEIGHT);

                    int imageSensorRotation = wikitudeCamera.getImageSensorRotation();
                    if (imageSensorRotation != 0) {
                        setImageSensorRotation(imageSensorRotation);
                    }
                }
            }
        });
    }

    /**
     * This is called via JNI from c++.
     * Will close the camera preview.
     */
    public void onInputPluginPaused() {
        Log.v(TAG, "onInputPluginPaused");

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                    wikitudeCamera2.close();
                } else {
                    wikitudeCamera.close();
                }
            }
        });
    }

    /**
     * This is called via JNI from c++.
     * Will start the camera preview.
     */
    @SuppressLint("NewApi")
    public void onInputPluginResumed() {
        Log.v(TAG, "onInputPluginResumed");

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                    SimpleInputPluginExtension.this.wikitudeCamera2.start(new ImageReader.OnImageAvailableListener() {
                        @Override
                        public void onImageAvailable(ImageReader reader) {
                            final Image image = reader.acquireLatestImage();

                            if (null != image && null != image.getPlanes()) {
                                final Image.Plane[] planes = image.getPlanes();

                                final int widthLuminance = image.getWidth();
                                final int heightLuminance = image.getHeight();

                                // 4:2:0 format -> chroma planes have half the width and half the height of the luma plane
                                final int widthChrominance = widthLuminance / 2;
                                final int heightChrominance = heightLuminance / 2;

                                final int pixelStrideLuminance = planes[0].getPixelStride();
                                final int rowStrideLuminance = planes[0].getRowStride();

                                final int pixelStrideBlue = planes[1].getPixelStride();
                                final int rowStrideBlue = planes[1].getRowStride();

                                final int pixelStrideRed = planes[2].getPixelStride();
                                final int rowStrideRed = planes[2].getRowStride();

                                notifyNewCameraFrame(
                                        widthLuminance,
                                        heightLuminance,
                                        getPlanePixelPointer(planes[0].getBuffer()),
                                        pixelStrideLuminance,
                                        rowStrideLuminance,
                                        widthChrominance,
                                        heightChrominance,
                                        getPlanePixelPointer(planes[1].getBuffer()),
                                        pixelStrideBlue,
                                        rowStrideBlue,
                                        getPlanePixelPointer(planes[2].getBuffer()),
                                        pixelStrideRed,
                                        rowStrideRed
                                );

                                image.close();
                            }
                        }
                    });
                } else {
                    wikitudeCamera.start(new Camera.PreviewCallback() {
                        @Override
                        public void onPreviewFrame(byte[] data, Camera camera) {
                            notifyNewCameraFrameN21(data);
                        }
                    });
                    setCameraFieldOfView(wikitudeCamera.getCameraFieldOfView());
                }
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
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                    wikitudeCamera2.close();
                } else {
                    wikitudeCamera.close();
                }
            }
        });
    }

    private byte[] getPlanePixelPointer(ByteBuffer pixelBuffer) {
        byte[] bytes;
        if (pixelBuffer.hasArray()) {
            bytes = pixelBuffer.array();
        } else {
            bytes = new byte[pixelBuffer.remaining()];
            pixelBuffer.get(bytes);
        }

        return bytes;
    }

    private native void initNative();
    private native void notifyNewCameraFrame(int widthLuminance, int heightLuminance, byte[] pixelPointerLuminance, int pixelStrideLuminance, int rowStrideLuminance, int widthChrominance, int heightChrominance, byte[] pixelPointerChromaBlue, int pixelStrideBlue, int rowStrideBlue, byte[] pixelPointerChromaRed, int pixelStrideRed, int rowStrideRed);
    private native void notifyNewCameraFrameN21(byte[] frameData);
    private native void setCameraFieldOfView(double fieldOfView);
    private native void setFrameSize(int frameWidth, int frameHeight);
    private native void setImageSensorRotation(int rotation);
}
