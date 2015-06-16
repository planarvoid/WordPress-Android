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

    private static final String ALPHA_PLAYBACK_LOGS_EMAIL = "android-alpha-logs-playback@soundcloud.com";
    private static final String BETA_PLAYBACK_LOGS_EMAIL = "android-beta-logs-playback@soundcloud.com";
    private static final String DEV_PLAYBACK_LOGS_EMAIL = "skippy@soundcloud.com";

    private static BuildType BUILD_TYPE;
    private static boolean VERBOSE_LOGGING;
    private static boolean GOOGLE_PLUS_ENABLED;

    private final String castReceiverAppId;
    @VisibleForTesting
    protected static final boolean IS_RUNNING_ON_DEVICE = Build.PRODUCT != null;
    @VisibleForTesting
    protected static final boolean IS_RUNNING_ON_EMULATOR = "google_sdk".equals(Build.PRODUCT) || "sdk".equals(Build.PRODUCT) ||
            "full_x86".equals(Build.PRODUCT) || "sdk_x86".equals(Build.PRODUCT);

    public String getFeedbackEmail() {
        return BUILD_TYPE.feedbackEmail;
    }

    public String getPlaybackFeedbackEmail() {
        return BUILD_TYPE.playbackFeedbackEmail;
    }


    public enum BuildType {
        DEBUG(DEV_LOGS_EMAIL, DEV_PLAYBACK_LOGS_EMAIL),
        ALPHA(ALPHA_LOGS_EMAIL, ALPHA_PLAYBACK_LOGS_EMAIL),
        BETA(BETA_LOGS_EMAIL, BETA_PLAYBACK_LOGS_EMAIL),
        RELEASE(null, null);

        private final String feedbackEmail;
        private final String playbackFeedbackEmail;

        BuildType(String feedbackEmail, String playbackFeedbackEmail) {

            this.feedbackEmail = feedbackEmail;
            this.playbackFeedbackEmail = playbackFeedbackEmail;
        }
    }

    @Inject
    public ApplicationProperties(Resources resources) {
        checkNotNull(resources, "Resources should not be null");
        String buildType = resources.getString(R.string.build_type);
        checkArgument(ScTextUtils.isNotBlank(buildType), "Build type not found in application package resources");
        BUILD_TYPE = BuildType.valueOf(buildType.toUpperCase(Locale.US));
        VERBOSE_LOGGING = resources.getBoolean(R.bool.verbose_logging);
        GOOGLE_PLUS_ENABLED = resources.getBoolean(R.bool.google_plus_enabled);
        castReceiverAppId = resources.getString(R.string.cast_receiver_app_id);
    }

    public boolean isGooglePlusEnabled() {
        return GOOGLE_PLUS_ENABLED;
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

    public boolean shouldEnableNetworkProxy() {
        return isDebugBuild() && IS_RUNNING_ON_DEVICE;
    }

    public boolean isRunningOnDevice() {
        return IS_RUNNING_ON_DEVICE;
    }

    public boolean isDevBuildRunningOnDevice() {
        return isDebugBuild() && IS_RUNNING_ON_DEVICE;
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

    public boolean shouldReportCrashes() {
        return !IS_RUNNING_ON_EMULATOR && IS_RUNNING_ON_DEVICE && !BuildType.DEBUG.equals(BUILD_TYPE) && BUILD_TYPE != null;
    }
}
