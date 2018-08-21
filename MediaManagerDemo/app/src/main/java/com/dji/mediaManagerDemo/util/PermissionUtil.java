package com.dji.mediaManagerDemo.util;

import com.wikitude.architect.ArchitectStartupConfiguration;

import android.Manifest;

public class PermissionUtil {

    private PermissionUtil(){}

    public static String[] getPermissionsForArFeatures(int features) {
        return (features & ArchitectStartupConfiguration.Features.Geo) == ArchitectStartupConfiguration.Features.Geo ?
                new String[]{Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION} :
                new String[]{Manifest.permission.CAMERA};
    }
}
