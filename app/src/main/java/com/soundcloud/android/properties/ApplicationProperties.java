package com.soundcloud.android.properties;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.soundcloud.android.R;
import com.soundcloud.android.R.string;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.res.Resources;
import android.os.Build;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Locale;

@Singleton
public class ApplicationProperties {

    public enum BuildType{
        DEBUG,
        BETA,
        RELEASE
    }

    private static BuildType BUILD_TYPE;
    private static boolean VERBOSE_LOGGING;
    //TODO Need to keep these static as we need to do more refactoring around ACRA reporting. Do not reference directly
    @VisibleForTesting
    protected static final boolean IS_RUNNING_ON_DEVICE = Build.PRODUCT != null;
    @VisibleForTesting
    protected static final boolean IS_RUNNING_ON_EMULATOR = "google_sdk".equals(Build.PRODUCT) || "sdk".equals(Build.PRODUCT) ||
            "full_x86".equals(Build.PRODUCT)   || "sdk_x86".equals(Build.PRODUCT);

    @Inject
    public ApplicationProperties(Resources resources){
        checkNotNull(resources, "Resources should not be null");
        String buildType = resources.getString(string.build_type);
        checkArgument(ScTextUtils.isNotBlank(buildType), "Build type not found in application package resources");
        BUILD_TYPE = BuildType.valueOf(buildType.toUpperCase(Locale.US));
        VERBOSE_LOGGING = resources.getBoolean(R.bool.verbose_logging);
    }

    public boolean useVerboseLogging() {
        return VERBOSE_LOGGING;
    }

    public boolean isReleaseBuild() {
        return BuildType.RELEASE.equals(BUILD_TYPE);
    }

    public boolean isDebugBuild() {
        return BuildType.DEBUG.equals(BUILD_TYPE);
    }

    public String getBuildType() {
        return BUILD_TYPE.name();
    }

    public boolean shouldEnableNetworkProxy(){
        return isDebugBuild() && IS_RUNNING_ON_DEVICE;
    }

    public boolean isRunningOnDevice(){
        return IS_RUNNING_ON_DEVICE;
    }

    public boolean isRunningOnEmulator(){
        return IS_RUNNING_ON_EMULATOR;
    }

    public boolean isDevBuildRunningOnDevice(){
        return isDebugBuild() && IS_RUNNING_ON_DEVICE;
    }

    public boolean shouldUseRichNotifications() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public boolean shouldUseBigNotifications() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("buildType", BUILD_TYPE).add("isDevice", IS_RUNNING_ON_DEVICE).
                add("isEmulator", IS_RUNNING_ON_EMULATOR).toString();
    }

    public static boolean shouldReportCrashes(){
        return !IS_RUNNING_ON_EMULATOR && IS_RUNNING_ON_DEVICE && !BuildType.DEBUG.equals(BUILD_TYPE) && BUILD_TYPE != null;
    }
}
