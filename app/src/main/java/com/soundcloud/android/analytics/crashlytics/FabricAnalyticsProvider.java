package com.soundcloud.android.analytics.crashlytics;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.soundcloud.android.analytics.AnalyticsProvider;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EncryptionErrorEvent;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.PaymentFailureEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UserSessionEvent;
import com.soundcloud.android.properties.ApplicationProperties;
import io.fabric.sdk.android.Fabric;

import android.util.Log;

import javax.inject.Inject;

public class FabricAnalyticsProvider implements AnalyticsProvider {

    private static final String TAG = "CrashlyticsLogger";

    private final boolean debugBuild;

    @Inject
    FabricAnalyticsProvider(ApplicationProperties applicationProperties) {
        debugBuild = applicationProperties.isDebugBuild();
    }

    @Override
    public void flush() {
    }

    @Override
    public void handleCurrentUserChangedEvent(CurrentUserChangedEvent event) {
    }

    @Override
    public void handleActivityLifeCycleEvent(ActivityLifeCycleEvent event) {
    }

    @Override
    public void handlePlaybackPerformanceEvent(PlaybackPerformanceEvent eventData) {
    }

    @Override
    public void handlePlaybackErrorEvent(PlaybackErrorEvent eventData) {
    }

    @Override
    public void handleOnboardingEvent(OnboardingEvent event) {
    }

    @Override
    public void handleTrackingEvent(TrackingEvent event) {
        if (Fabric.isInitialized()) {
            logWithCrashlytics(event);
            trackWithAnswers(event);
        }
    }

    private boolean shouldIncludeInCrashlyticsLogs(TrackingEvent event) {
        return event instanceof ScreenEvent || event instanceof UIEvent;
    }

    private void logWithCrashlytics(TrackingEvent event) {
        if (shouldIncludeInCrashlyticsLogs(event)) {
            if (debugBuild) {
                Crashlytics.log(Log.DEBUG, TAG, event.toString());
            } else {
                Crashlytics.log(event.toString());
            }
        }
    }

    private void trackWithAnswers(TrackingEvent event) {
        if (event instanceof PaymentFailureEvent) {
            trackPaymentFailure((PaymentFailureEvent) event);
        } else if (event instanceof EncryptionErrorEvent) {
            trackEncryptionError((EncryptionErrorEvent) event);
        }
    }

    private void trackEncryptionError(EncryptionErrorEvent event) {
        Answers.getInstance().logCustom(
                new CustomEvent("Encryption error")
                        .putCustomAttribute("Kind", event.getKind())
                        .putCustomAttribute("ErrorMessage", event.getMessage())
                        .putCustomAttribute("DetailMessage", event.getDetailMessage())
        );
    }

    private void trackPaymentFailure(PaymentFailureEvent event) {
        Answers.getInstance().logCustom(
                new CustomEvent("Payment failure")
                        .putCustomAttribute("Reason", event.getReason())
        );
    }

    @Override
    public void handleUserSessionEvent(UserSessionEvent event) {
    }

}
