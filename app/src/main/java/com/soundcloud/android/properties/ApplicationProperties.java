package com.soundcloud.android.properties;


import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
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
    //TODO Need to keep these static as we need to do more refactoring around ACRA reporting. Do not reference directly
    @VisibleForTesting
    protected static final boolean IS_RUNNING_ON_DALVIK = Build.PRODUCT != null;
    @VisibleForTesting
    protected static final boolean IS_RUNNING_ON_EMULATOR = "google_sdk".equals(Build.PRODUCT) || "sdk".equals(Build.PRODUCT) ||
            "full_x86".equals(Build.PRODUCT)   || "sdk_x86".equals(Build.PRODUCT);


    @Inject
    public ApplicationProperties(Resources resources){
        checkNotNull(resources, "Resources should not be null");
        String buildType = resources.getString(string.build_type);
        checkArgument(ScTextUtils.isNotBlank(buildType), "Build type not found in application package resources");
        BUILD_TYPE = BuildType.valueOf(buildType.toUpperCase(Locale.US));
    }

    public boolean isReleaseBuild() {
        return BuildType.RELEASE.equals(BUILD_TYPE);
    }

    public boolean isDebugBuild() {
        return BuildType.DEBUG.equals(BUILD_TYPE);
    }

    public boolean isBetaBuild() {
        return BuildType.BETA.equals(BUILD_TYPE);
    }

    public String getBuildType() {
        return BUILD_TYPE.name();
    }

    public boolean shouldEnableNetworkProxy(){
        return isDebugBuild() && IS_RUNNING_ON_DALVIK;
    }

    public boolean isRunningOnDalvik(){
        return IS_RUNNING_ON_DALVIK;
    }

    public boolean isRunningOnEmulator(){
        return IS_RUNNING_ON_EMULATOR;
    }

    public boolean isDevBuildRunningOnDalvik(){
        return isDebugBuild() && IS_RUNNING_ON_DALVIK;
    }

    public boolean isBetaBuildRunningOnDalvik(){
        return isBetaBuild() && IS_RUNNING_ON_DALVIK;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("buildType", BUILD_TYPE).add("isDalvik", IS_RUNNING_ON_DALVIK).
                add("isEmulator", IS_RUNNING_ON_EMULATOR).toString();
    }

    public static boolean shouldReportCrashes(){
        return !IS_RUNNING_ON_EMULATOR && IS_RUNNING_ON_DALVIK && !BuildType.DEBUG.equals(BUILD_TYPE);
    }
}
