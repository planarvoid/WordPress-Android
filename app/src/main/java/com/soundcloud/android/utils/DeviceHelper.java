package com.soundcloud.android.utils;

import com.soundcloud.android.BuildConfig;
import com.soundcloud.java.hashing.Hashing;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.strings.Strings;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DeviceHelper {

    private static final String UNKNOWN_DEVICE = "unknown device";

    private final Context context;
    private final BuildHelper buildHelper;

    private String udid;

    @Inject
    public DeviceHelper(Context context, BuildHelper buildHelper) {
        this.context = context;
        this.buildHelper = buildHelper;
        generateUdid();
    }

    private void generateUdid() {
        String id = getUniqueDeviceId();
        if (Strings.isNotBlank(id)) {
            udid = Hashing.md5(id);
        }
    }

    public boolean hasUdid() {
        return Strings.isNotBlank(udid);
    }

    /**
     * @return a unique id for this device (MD5 of IMEI / {@link android.provider.Settings.Secure#ANDROID_ID}) or null
     */
    @Nullable
    public String getUdid() {
        return udid;
    }

    private String getUniqueDeviceId() {
        TelephonyManager tmgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String id = tmgr == null ? null : tmgr.getDeviceId();
        if (Strings.isBlank(id)) {
            id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        return id;
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
            return UNKNOWN_DEVICE;
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
