package com.soundcloud.android.utils;

import com.soundcloud.android.R;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import javax.inject.Inject;

public class DeviceHelper {

    private static final String UNKNOWN_VERSION = "unknown version";
    private static final String UNKNOWN_DEVICE = "unknown device";
    private static final int UNKNOWN_VERSION_CODE = 0;

    private final Context context;

    @Inject
    public DeviceHelper(Context context) {
        this.context = context;
    }

    /**
     * @return a unique id for this device (MD5 of IMEI / {@link android.provider.Settings.Secure#ANDROID_ID}) or null
     */
    public String getUniqueDeviceID() {
        TelephonyManager tmgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String id = tmgr == null ? null : tmgr.getDeviceId();
        if (ScTextUtils.isBlank(id)) {
            id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        return ScTextUtils.isBlank(id) ? null : IOUtils.md5(id);
    }

    public String getDeviceName(){
        final String manufacturer = Build.MANUFACTURER;
        final String model = Build.MODEL;
        if (ScTextUtils.isNotBlank(model)) {
            if (ScTextUtils.isNotBlank(manufacturer)) {
                return model.startsWith(manufacturer) ? model : manufacturer + " " + model;
            } else {
                return model;
            }
        } else if (ScTextUtils.isNotBlank(manufacturer)) {
            return manufacturer;
        } else {
            return UNKNOWN_DEVICE;
        }
    }

    public String getAppVersion() {
        PackageInfo packageInfo = getPackageInfo();
        return packageInfo != null ? packageInfo.versionName : UNKNOWN_VERSION;
    }

    public String getUserVisibleVersion() {
        PackageInfo packageInfo = getPackageInfo();
        return packageInfo != null ? packageInfo.versionName : context.getString(R.string.unavailable);
    }

    public int getAppVersionCode() {
        PackageInfo packageInfo = getPackageInfo();
        return packageInfo != null ? packageInfo.versionCode : UNKNOWN_VERSION_CODE;
    }

    public String getPackageName() {
        return context.getPackageName();
    }

    public String getUserAgent() {
        return String.format("SoundCloud-Android/%s (Android %s; %s)",
                getAppVersion(),
                String.valueOf(Build.VERSION.RELEASE),
                getDeviceName());
    }

    @Nullable
    private PackageInfo getPackageInfo() {
        final String packageName = getPackageName();
        final PackageManager packageManager = context.getPackageManager();
        if (packageManager != null) {
            try {
                return packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA);

            } catch (PackageManager.NameNotFoundException ignored) {
                Log.e("Unexpected name not found exception " + packageName);
            }
        }
        return null;
    }

    public int getCurrentOrientation() {
        return context.getResources().getConfiguration().orientation;
    }
}
