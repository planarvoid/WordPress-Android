package com.soundcloud.android.utils;

import com.google.common.hash.Hashing;
import com.soundcloud.android.R;
import com.soundcloud.java.objects.MoreObjects;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DeviceHelper {

    private static final String UNKNOWN_VERSION = "unknown version";
    private static final String UNKNOWN_DEVICE = "unknown device";
    private static final int UNKNOWN_VERSION_CODE = 0;

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
        if (ScTextUtils.isNotBlank(id)) {
            // We still use IOUtils here instead of guava because its a different algorithm, and tracking needs the legacy values
            udid = IOUtils.md5(id);
        }
    }

    public boolean hasUdid() {
        return ScTextUtils.isNotBlank(udid);
    }

    /**
     * @return a unique id for this device (MD5 of IMEI / {@link android.provider.Settings.Secure#ANDROID_ID}) or null
     */
    @Nullable
    public String getUdid() {
        return udid;
    }
    
    public boolean inSplitTestGroup(){
        String id = getUniqueDeviceId();
        final long idAsLong = Hashing.md5().hashString(id).asLong();
        return !ScTextUtils.isBlank(id) && (int) (Math.abs(idAsLong) % 2) == 1;
    }

    private String getUniqueDeviceId() {
        TelephonyManager tmgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String id = tmgr == null ? null : tmgr.getDeviceId();
        if (ScTextUtils.isBlank(id)) {
            id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        return id;
    }

    public String getDeviceName(){
        final String manufacturer = buildHelper.getManufacturer();
        final String model = buildHelper.getModel();
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
                String.valueOf(buildHelper.getAndroidReleaseVersion()),
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

    public boolean hasMicrophone() {
        PackageManager pm = context.getPackageManager();
        return pm != null && pm.hasSystemFeature(PackageManager.FEATURE_MICROPHONE);
    }



    public int getCurrentOrientation() {
        return context.getResources().getConfiguration().orientation;
    }

    public static String getBuildInfo(){
        return MoreObjects.toStringHelper("Build")
                .add("Brand",Build.BRAND)
                .add("Device",Build.DEVICE)
                .add("Hardware",Build.HARDWARE)
                .add("Manufacturer",Build.MANUFACTURER)
                .add("Model",Build.MODEL)
                .add("Product",Build.PRODUCT)
                .add("Type",Build.TYPE)
                .toString();
    }
}
