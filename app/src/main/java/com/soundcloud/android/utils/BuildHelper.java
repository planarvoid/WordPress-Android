package com.soundcloud.android.utils;

import android.os.Build;

import javax.inject.Inject;

// wrapper so we can test against Build properties
public class BuildHelper {

    @Inject
    public BuildHelper() {
        // dagger
    }

    public String getManufacturer() {
        return Build.MANUFACTURER;
    }

    public String getModel() {
        return Build.MODEL;
    }

    public String getAndroidReleaseVersion() {
        return Build.VERSION.RELEASE;
    }
}
