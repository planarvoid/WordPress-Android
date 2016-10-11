package com.soundcloud.android.utils;

import com.soundcloud.android.BuildConfig;
import com.soundcloud.android.R;
import com.soundcloud.java.hashing.Hashing;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.strings.Strings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.media.CamcorderProfile;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DeviceHelper {

    private static final int LOW_MEM_DEVICE_THRESHOLD = 50 * 1024 * 1024; // Available memory (bytes)

    private static final String MISSING_DEVICE_NAME = "unknown device";
    private static final String MISSING_UDID = "unknown";

    private final Context context;
    private final BuildHelper buildHelper;
    private final Resources resources;
    private final String udid;

    @Inject
    public DeviceHelper(Context context, BuildHelper buildHelper, Resources resources) {
        this.context = context;
        this.buildHelper = buildHelper;
        this.resources = resources;
        final String deviceId = getUniqueDeviceId();
        if (deviceId != null) {
            this.udid = Hashing.md5(deviceId);
        } else {
            this.udid = MISSING_UDID;
        }
    }

    @SuppressLint("HardwareIds")
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

    public String getAppVersionName() {
        return BuildConfig.VERSION_NAME;
    }

    public int getAppVersionCode() {
        return BuildConfig.VERSION_CODE;
    }

    public int getCurrentOrientation() {
        return context.getResources().getConfiguration().orientation;
    }

    public boolean hasCamcorderProfile(int camcorderProfile) {
        return CamcorderProfile.hasProfile(camcorderProfile);
    }

    public boolean isOrientation(int orientation) {
        return getCurrentOrientation() == orientation;
    }

    public boolean isLowMemoryDevice() {
        // Reference values for available mem: Wildfire: 16,777,216; Nexus S: 33,554,432; Nexus 4: 201,326,592
        return Runtime.getRuntime().maxMemory() < LOW_MEM_DEVICE_THRESHOLD;
    }

    public boolean isTablet() {
        return resources.getBoolean(R.bool.is_tablet);
    }

    public DisplayMetrics getDisplayMetrics() {
        DisplayMetrics metrics = new DisplayMetrics();
        ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getMetrics(metrics);
        return metrics;
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
