package com.dji.mediaManagerDemo.advanced.plugins;

import com.dji.mediaManagerDemo.R;
import com.wikitude.architect.ArchitectView;
import com.wikitude.common.plugins.PluginManager;
import com.dji.mediaManagerDemo.advanced.ArchitectViewExtension;


import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * This Extension is the java counterpart of the 13_PluginsAPI_5_MarkerTracking AR-Experience.
 * It registers a native Plugin which uses camera frames provided by the Wikitude SDK for
 * aruco marker detection and will render the augmentations using the Wikitude positionables api.
 */
public class MarkerTrackingPluginExtension extends ArchitectViewExtension {

    private static final String TAG = "MarkerTrackingPlugin";

    public MarkerTrackingPluginExtension(Activity activity, ArchitectView architectView) {
        super(activity, architectView);
    }

    @Override
    public void onPostCreate() {
        /*
         * Registers the plugin with the name "markertracking".
         * The library containing the native plugin is libwikitudePlugins.so.
         */
        architectView.registerNativePlugins("wikitudePlugins", "markertracking", new PluginManager.PluginErrorCallback() {
            @Override
            public void onRegisterError(int errorCode, String errorMessage) {
                Toast.makeText(activity, R.string.error_loading_plugins, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Plugin failed to load. Reason: " + errorMessage);
            }
        });

        initNative();

        if (isCameraLandscape()) {
            setDefaultDeviceOrientationLandscape(true);
        }
    }

    public boolean isCameraLandscape(){
        final Display display = ((WindowManager)activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        final DisplayMetrics dm = new DisplayMetrics();
        final int rotation = display.getRotation();

        display.getMetrics(dm);

        final boolean is90off = rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270;
        final boolean isLandscape = dm.widthPixels > dm.heightPixels;

        return is90off ^ isLandscape;
    }

    private native void initNative();
    private native void setDefaultDeviceOrientationLandscape(boolean isLandscape);
}
