package com.soundcloud.android.properties;

import static com.soundcloud.java.checks.Preconditions.checkArgument;
import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import com.soundcloud.android.R;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.strings.Strings;

import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.VisibleForTesting;

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

    private static BuildType buildType;
    private boolean verboseLogging;
    private boolean googlePlusEnabled;
    private boolean enforceConcurrentStreamingLimitation;
    private boolean logQueries;

    private final String castReceiverAppId;
    @VisibleForTesting
    static final boolean IS_RUNNING_ON_DEVICE = Build.PRODUCT != null;
    @VisibleForTesting
    static final boolean IS_RUNNING_ON_EMULATOR = "google_sdk".equals(Build.PRODUCT) || "sdk".equals(Build.PRODUCT) ||
            "full_x86".equals(Build.PRODUCT) || "sdk_x86".equals(Build.PRODUCT);

    public String getFeedbackEmail() {
        return buildType.feedbackEmail;
    }

    public String getPlaybackFeedbackEmail() {
        return buildType.playbackFeedbackEmail;
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
        checkArgument(Strings.isNotBlank(buildType), "Build type not found in application package resources");
        ApplicationProperties.buildType = BuildType.valueOf(buildType.toUpperCase(Locale.US));
        verboseLogging = resources.getBoolean(R.bool.verbose_logging);
        googlePlusEnabled = resources.getBoolean(R.bool.google_plus_enabled);
        enforceConcurrentStreamingLimitation = resources.getBoolean(R.bool.enforce_concurrent_streaming_limitation);
        castReceiverAppId = resources.getString(R.string.cast_receiver_app_id);
        logQueries = resources.getBoolean(R.bool.log_queries);
    }

    public boolean isGooglePlusEnabled() {
        return googlePlusEnabled;
    }

    public boolean useVerboseLogging() {
        return verboseLogging;
    }

    public boolean shouldLogQueries() {
        return logQueries;
    }

    public boolean enforceConcurrentStreamingLimitation() {
        return enforceConcurrentStreamingLimitation;
    }

    public boolean isReleaseBuild() {
        return BuildType.RELEASE.equals(buildType);
    }

    public boolean isDebugBuild() {
        return BuildType.DEBUG.equals(buildType);
    }

    public boolean isAlphaBuild() {
        return BuildType.ALPHA.equals(buildType);
    }

    public boolean shouldAllowFeedback() {
        return BuildType.ALPHA.equals(buildType) || BuildType.BETA.equals(buildType) || BuildType.DEBUG.equals(
                buildType);
    }

    public boolean allowDatabaseMigrationsSilentErrors() {
        return BuildType.RELEASE.equals(buildType) || BuildType.BETA.equals(buildType);
    }

    public String getBuildType() {
        return buildType.name();
    }

    public boolean shouldEnableNetworkProxy() {
        return isDebugBuild() && IS_RUNNING_ON_DEVICE;
    }

    public boolean isDevelopmentMode() {
        return isDebugBuild();
    }

    public boolean isDevBuildRunningOnDevice() {
        return isDebugBuild() && IS_RUNNING_ON_DEVICE;
    }

    public boolean canAccessCodecInformation() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    public boolean canReattachSurfaceTexture() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    public boolean canUseMoatForAdViewability() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1;
    }

    public String getCastReceiverAppId() {
        return castReceiverAppId;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("buildType", buildType).add("isDevice", IS_RUNNING_ON_DEVICE).
                add("isEmulator", IS_RUNNING_ON_EMULATOR).toString();
    }

    public boolean shouldReportCrashes() {
        return !IS_RUNNING_ON_EMULATOR && IS_RUNNING_ON_DEVICE && !BuildType.DEBUG.equals(buildType) && buildType != null;
    }
}
