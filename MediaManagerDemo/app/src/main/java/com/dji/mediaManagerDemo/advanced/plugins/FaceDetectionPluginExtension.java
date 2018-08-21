package com.dji.mediaManagerDemo.advanced.plugins;

import com.wikitude.architect.ArchitectView;
import com.wikitude.common.plugins.PluginManager;
import com.dji.mediaManagerDemo.advanced.ArchitectViewExtension;
import com.dji.mediaManagerDemo.R;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This Extension is the java counterpart of the 13_PluginsAPI_2_FaceDetection AR-Experience.
 * It registers a native Plugin which uses camera frames provided by the Wikitude SDK for
 * face detection and will render the augmentations in java.
 */
public class FaceDetectionPluginExtension extends ArchitectViewExtension {

    private static final String TAG = "FaceDetectionPlugin";

    private final Object projectionMatrixLock = new Object();

    private StrokedRectangle rectangle = new StrokedRectangle(StrokedRectangle.Type.FACE);
    private int defaultOrientation;
    private boolean projectionMatrixUpdateRequired = false;
    private float[] projectionMatrix = new float[16];

    public FaceDetectionPluginExtension(Activity activity, ArchitectView architectView) {
        super(activity, architectView);
    }

    @Override
    public void onPostCreate() {
        /*
         * Registers the plugin with the name "face_detection".
         * The library containing the native plugin is libwikitudePlugins.so.
         */
        architectView.registerNativePlugins("wikitudePlugins", "face_detection", new PluginManager.PluginErrorCallback() {
            @Override
            public void onRegisterError(int errorCode, String errorMessage) {
                Toast.makeText(activity, R.string.error_loading_plugins, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Plugin failed to load. Reason: " + errorMessage);
            }
        });

        try {
            // load cascade file from application resources
            final InputStream is = activity.getResources().openRawResource(R.raw.high_database);
            final File cascadeDir = activity.getDir("cascade", Context.MODE_PRIVATE);
            final File _cascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            final FileOutputStream os = new FileOutputStream(_cascadeFile);

            final byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            setDatabase(_cascadeFile.getAbsolutePath());
            cascadeDir.delete();

        } catch (IOException e) {
            e.printStackTrace();
        }

        evaluateDeviceDefaultOrientation();
        if (defaultOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            setIsBaseOrientationLandscape(true);
        }
    }

    @Override
    public void onResume() {
        if (rectangle == null) {

            rectangle = new StrokedRectangle(StrokedRectangle.Type.FACE);
        }
        initNative();
    }

    @Override
    public void onPause() {
        destroyNative();
        rectangle = null;
    }

    public void evaluateDeviceDefaultOrientation() {
        final WindowManager windowManager =  (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        final Configuration config = activity.getResources().getConfiguration();

        int rotation = windowManager.getDefaultDisplay().getRotation();

        if ( ((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) &&
                config.orientation == Configuration.ORIENTATION_LANDSCAPE)
                || ((rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) &&
                config.orientation == Configuration.ORIENTATION_PORTRAIT)) {
            defaultOrientation = Configuration.ORIENTATION_LANDSCAPE;
        } else {
            defaultOrientation = Configuration.ORIENTATION_PORTRAIT;
        }
    }

    /**
     * called from c++ plugin
     */
    public void onFaceDetected(float[] modelViewMatrix) {
        if (rectangle != null) {

            rectangle.setViewMatrix(modelViewMatrix);
        }
    }

    /**
     * called from c++ plugin
     */
    public void onFaceLost() {

        if (rectangle != null) {

            rectangle.onFaceLost();
        }
    }

    /**
     * called from c++ plugin
     */
    public void onProjectionMatrixChanged(float[] projectionMatrix) {

        synchronized (projectionMatrixLock) {
            projectionMatrixUpdateRequired = true;
            this.projectionMatrix = projectionMatrix;
        }
    }

    /**
     * called from c++ plugin
     */
    public void renderDetectedFaceAugmentation() {

        if (rectangle != null) {

            synchronized (projectionMatrixLock) {
                if (projectionMatrixUpdateRequired) {
                    rectangle.setProjectionMatrix(projectionMatrix);
                    projectionMatrixUpdateRequired = false;
                }
            }

            rectangle.onDrawFrame();
        }
    }

    private native void initNative();
    private native void destroyNative();
    private native void setDatabase(String casecadeFilePath);
    private native void setIsBaseOrientationLandscape(boolean isBaseOrientationLandscape_);
}
