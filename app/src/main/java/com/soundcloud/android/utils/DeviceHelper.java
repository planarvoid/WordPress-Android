package com.soundcloud.android.utils;

import com.soundcloud.android.BuildConfig;
import com.soundcloud.java.hashing.Hashing;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.strings.Strings;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DeviceHelper {

    private static final String MISSING_DEVICE_NAME = "unknown device";
    private static final String MISSING_UDID = "unknown";

    private final Context context;
    private final BuildHelper buildHelper;
    private final String udid;

    @Inject
    public DeviceHelper(Context context, BuildHelper buildHelper) {
        this.context = context;
        this.buildHelper = buildHelper;
        final String deviceId = getUniqueDeviceId();
        if (deviceId != null) {
            this.udid = Hashing.md5(deviceId);
        } else {
            this.udid = MISSING_UDID;
        }
    }

    private String getUniqueDeviceId() {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    /**
     * @return a unique id for this device (MD5 of {@link android.provider.Settings.Secure#ANDROID_ID}) or "unknown"
     */
    public String getUdid() {
        return udid;
    }

    public String getDeviceName() {
        final String manufacturer = buildHelper.getManufacturer();
        final String model = buildHelper.getModel();
        if (Strings.isNotBlank(model)) {
            if (Strings.isNotBlank(manufacturer)) {
                return model.startsWith(manufacturer) ? model : manufacturer + " " + model;
            } else {
                return model;
            }
        } else if (Strings.isNotBlank(manufacturer)) {
            return manufacturer;
        } else {
            return MISSING_DEVICE_NAME;
        }
    }

    public String getPackageName() {
        return context.getPackageName();
    }

    public String getUserAgent() {
        return String.format("SoundCloud-Android/%s (Android %s; %s)",
                BuildConfig.VERSION_NAME,
                String.valueOf(buildHelper.getAndroidReleaseVersion()),
                getDeviceName());
    }

    public String getAppVersion() {
        return BuildConfig.VERSION_NAME;
    }

    public boolean hasMicrophone() {
        PackageManager pm = context.getPackageManager();
        return pm != null && pm.hasSystemFeature(PackageManager.FEATURE_MICROPHONE);
    }

    public int getCurrentOrientation() {
        return context.getResources().getConfiguration().orientation;
    }

    public static String getBuildInfo() {
        return MoreObjects.toStringHelper("Build")
                .add("Brand", Build.BRAND)
                .add("Device", Build.DEVICE)
                .add("Hardware", Build.HARDWARE)
                .add("Manufacturer", Build.MANUFACTURER)
                .add("Model", Build.MODEL)
                .add("Product", Build.PRODUCT)
                .add("Type", Build.TYPE)
                .toString();
    }
}
