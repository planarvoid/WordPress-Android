package com.soundcloud.android.properties;


import android.content.res.Resources;
import android.os.Build;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.soundcloud.android.R.string;
import com.soundcloud.android.utils.ScTextUtils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class ApplicationProperties {

    public enum BuildType{
        DEBUG,
        BETA,
        STAGING,
        RELEASE
    }

    private static BuildType mBuildType;
    //TODO Need to keep these static as we need to do more refactoring around ACRA reporting. Do not reference directly
    @VisibleForTesting
    protected static final boolean mIsRunningOnDalvik = Build.PRODUCT != null;
    @VisibleForTesting
    protected static final boolean mIsRunningOnEmulator = "google_sdk".equals(Build.PRODUCT) || "sdk".equals(Build.PRODUCT) ||
            "full_x86".equals(Build.PRODUCT)   || "sdk_x86".equals(Build.PRODUCT);


    public ApplicationProperties(Resources resources){
        checkNotNull(resources, "Resources should not be null");
        String buildType = resources.getString(string.build_type);
        checkArgument(ScTextUtils.isNotBlank(buildType), "Build type not found in application package resources");
        mBuildType = BuildType.valueOf(buildType.toUpperCase());
    }

    @VisibleForTesting
    protected boolean isReleaseBuild() {
        return BuildType.RELEASE.equals(mBuildType);
    }


    @VisibleForTesting
    protected boolean isDebugBuild(){
        return BuildType.DEBUG.equals(mBuildType);
    }

    @VisibleForTesting
    protected boolean isBetaBuild(){
        return BuildType.BETA.equals(mBuildType);
    }

    public String getBuildType() {
        return mBuildType.name();
    }

    public boolean shouldEnableNetworkProxy(){
        return isDebugBuild() && mIsRunningOnDalvik;
    }

    public boolean isRunningOnDalvik(){
        return mIsRunningOnDalvik;
    }

    public boolean isDevBuildRunningOnDalvik(){
        return isDebugBuild() && mIsRunningOnDalvik;
    }

    public boolean isBetaBuildRunningOnDalvik(){
        return isBetaBuild() && mIsRunningOnDalvik;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("buildType", mBuildType).add("isDalvik", mIsRunningOnDalvik).
                add("isEmulator", mIsRunningOnEmulator).toString();
    }

    //DO NOT USE THIS - Temporary until we remove ATInternet stuff
    public static boolean shouldReportToAcra(){
        return !mIsRunningOnEmulator && mIsRunningOnDalvik && !BuildType.DEBUG.equals(mBuildType);
    }
}
