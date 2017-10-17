package com.soundcloud.android.properties;

import com.soundcloud.android.BuildConfig;
import com.soundcloud.android.R;
import com.soundcloud.java.objects.MoreObjects;
import org.jetbrains.annotations.NotNull;

import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;
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
    private boolean registerForGcm;
    private boolean logQueries;
    private boolean failFastOnMappingExceptions;

    @VisibleForTesting
    static final boolean IS_RUNNING_ON_DEVICE = Build.PRODUCT != null;
    @VisibleForTesting
    static final boolean IS_RUNNING_ON_EMULATOR = "google_sdk".equals(Build.PRODUCT) || "sdk".equals(Build.PRODUCT) ||
            "full_x86".equals(Build.PRODUCT) || "sdk_x86".equals(Build.PRODUCT) || "google_sdk_x86".equals(Build.PRODUCT);

    private static final List<String> BUILD_TYPES_BETA_AND_BELOW = Arrays.asList(BuildType.ALPHA.name().toLowerCase(Locale.US),
                                                                                 BuildType.BETA.name().toLowerCase(Locale.US),
                                                                                 BuildType.DEBUG.name().toLowerCase(Locale.US));

    private static final List<String> BUILD_TYPES_ALPHA_AND_BELOW = Arrays.asList(BuildType.ALPHA.name().toLowerCase(Locale.US),
                                                                                  BuildType.DEBUG.name().toLowerCase(Locale.US));

    public String getFeedbackEmail() {
        return buildType.feedbackEmail;
    }

    public String getPlaybackFeedbackEmail() {
        return buildType.playbackFeedbackEmail;
    }

    @SuppressWarnings("sc.EnumUsage")
    private enum BuildType {
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
    public ApplicationProperties(@NotNull Resources resources) {
        ApplicationProperties.buildType = BuildType.valueOf(resources.getString(R.string.build_type).toUpperCase(Locale.US));
        verboseLogging = resources.getBoolean(R.bool.verbose_logging);
        googlePlusEnabled = resources.getBoolean(R.bool.google_plus_enabled);
        enforceConcurrentStreamingLimitation = resources.getBoolean(R.bool.enforce_concurrent_streaming_limitation);
        registerForGcm = resources.getBoolean(R.bool.register_for_gcm);
        logQueries = resources.getBoolean(R.bool.log_queries);
        failFastOnMappingExceptions = resources.getBoolean(R.bool.fail_fast_on_mapping_exceptions);
    }

    public static boolean isBetaOrBelow() {
        return BUILD_TYPES_BETA_AND_BELOW.contains(BuildConfig.BUILD_TYPE);
    }

    public static boolean isAlphaOrBelow() {
        return BUILD_TYPES_ALPHA_AND_BELOW.contains(BuildConfig.BUILD_TYPE);
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

    public boolean shouldFailFastOnMappingExceptions() {
        return failFastOnMappingExceptions;
    }

    public boolean enforceConcurrentStreamingLimitation() {
        return enforceConcurrentStreamingLimitation;
    }

    public boolean registerForGcm() {
        return registerForGcm;
    }

    public boolean isReleaseBuild() {
        return isBuildType(BuildType.RELEASE);
    }

    public boolean isDebuggableFlavor() {
        return isBuildType(BuildType.DEBUG, BuildType.ALPHA);
    }

    public boolean isBetaBuild() {
        return isBuildType(BuildType.BETA);
    }

    boolean isAlphaBuild() {
        return isBuildType(BuildType.ALPHA);
    }

    private boolean isBuildType(BuildType... types) {
        return Arrays.asList(types).contains(buildType);
    }

    public boolean shouldAllowFeedback() {
        return isBuildType(BuildType.ALPHA, BuildType.BETA, BuildType.DEBUG);
    }

    public boolean allowDatabaseMigrationsSilentErrors() {
        return isBuildType(BuildType.RELEASE, BuildType.BETA);
    }

    public String getBuildTypeName() {
        return buildType.name();
    }

    public boolean isDevelopmentMode() {
        return isDebuggableFlavor();
    }

    public boolean isDevBuildRunningOnDevice() {
        return isDebuggableFlavor() && IS_RUNNING_ON_DEVICE;
    }

    public boolean canChangeOfflineContentLocation() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    public boolean canMediaPlayerSupportVideoHLS() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("buildType", buildType).add("isDevice", IS_RUNNING_ON_DEVICE).
                add("isEmulator", IS_RUNNING_ON_EMULATOR).toString();
    }

    public boolean shouldReportCrashes() {
        return !IS_RUNNING_ON_EMULATOR && IS_RUNNING_ON_DEVICE && buildType != null && !isBuildType(BuildType.DEBUG);
    }

    public boolean shouldReportNativeCrashes() {
        // The Crashlytics NDK dependency is only included in Beta since it sometimes causes an ANR after a native crash.
        return shouldReportCrashes() && isBuildType(BuildType.BETA);
    }
}
