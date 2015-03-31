package com.soundcloud.android.properties;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.soundcloud.android.R;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.res.Resources;
import android.os.Build;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Locale;

@Singleton
public class ApplicationProperties {

    private static final String ALPHA_LOGS_EMAIL = "android-alpha-logs@soundcloud.com";
    private static final String BETA_LOGS_EMAIL = "android-beta-logs@soundcloud.com";
    private static final String DEV_LOGS_EMAIL = "android-dev@soundcloud.com";

    private static BuildType BUILD_TYPE;
    private static boolean VERBOSE_LOGGING;
    private final String castReceiverAppId;

    @VisibleForTesting
    protected static final boolean IS_RUNNING_ON_DEVICE = Build.PRODUCT != null;
    @VisibleForTesting
    protected static final boolean IS_RUNNING_ON_EMULATOR = "google_sdk".equals(Build.PRODUCT) || "sdk".equals(Build.PRODUCT) ||
            "full_x86".equals(Build.PRODUCT)   || "sdk_x86".equals(Build.PRODUCT);

    public String getFeedbackEmail() {
        switch (BUILD_TYPE){
            case ALPHA:
                return ALPHA_LOGS_EMAIL;
            case BETA:
                return BETA_LOGS_EMAIL;
            case DEBUG:
                return DEV_LOGS_EMAIL;
            default:
                return null;
        }
    }

    public enum BuildType {
        DEBUG,
        ALPHA,
        BETA,
        RELEASE
    }

    @Inject
    public ApplicationProperties(Resources resources){
        checkNotNull(resources, "Resources should not be null");
        String buildType = resources.getString(R.string.build_type);
        checkArgument(ScTextUtils.isNotBlank(buildType), "Build type not found in application package resources");
        BUILD_TYPE = BuildType.valueOf(buildType.toUpperCase(Locale.US));
        VERBOSE_LOGGING = resources.getBoolean(R.bool.verbose_logging);
        castReceiverAppId = resources.getString(R.string.cast_receiver_app_id);
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

    public boolean isAlphaBuild() {
        return BuildType.ALPHA.equals(BUILD_TYPE);
    }

    public boolean shouldAllowFeedback() {
        return BuildType.ALPHA.equals(BUILD_TYPE) || BuildType.BETA.equals(BUILD_TYPE) || BuildType.DEBUG.equals(BUILD_TYPE);
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

    public boolean shouldUseMediaStyleNotifications() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public String getCastReceiverAppId() {
        return castReceiverAppId;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("buildType", BUILD_TYPE).add("isDevice", IS_RUNNING_ON_DEVICE).
                add("isEmulator", IS_RUNNING_ON_EMULATOR).toString();
    }

    public boolean shouldReportCrashes(){
        return !IS_RUNNING_ON_EMULATOR && IS_RUNNING_ON_DEVICE && !BuildType.DEBUG.equals(BUILD_TYPE) && BUILD_TYPE != null;
    }
}
