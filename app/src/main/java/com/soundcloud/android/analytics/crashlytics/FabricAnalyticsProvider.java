package com.soundcloud.android.analytics.crashlytics;

import com.crashlytics.android.core.CrashlyticsCore;
import com.soundcloud.android.analytics.AnalyticsProvider;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.MetricEvent;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.reporting.DatabaseReporting;
import com.soundcloud.reporting.DataPoint;
import com.soundcloud.reporting.FabricReporter;
import com.soundcloud.reporting.Metric;

import android.content.Context;
import android.util.Log;

import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicBoolean;

public class FabricAnalyticsProvider implements AnalyticsProvider {

    private static final String TAG = "FabricAnalytics";
    private static final String RECORD_COUNT_METRIC = "DB:RecordCount";

    private final boolean debugBuild;
    private final AtomicBoolean pendingOnCreate = new AtomicBoolean();

    private final DatabaseReporting databaseReporting;
    private final FabricReporter fabricReporter;
    private final FabricProvider fabricProvider;

    @Inject
    FabricAnalyticsProvider(ApplicationProperties applicationProperties,
                            FabricProvider fabricProvider,
                            DatabaseReporting databaseReporting,
                            FabricReporter fabricReporter) {
        this.fabricProvider = fabricProvider;
        this.databaseReporting = databaseReporting;
        this.fabricReporter = fabricReporter;
        this.debugBuild = applicationProperties.isDebugBuild();
    }

    @Override
    public void flush() {
        if (fabricProvider.isInitialized() && pendingOnCreate.getAndSet(false)) {
            reportDatabaseMetrics();
        }
    }

    protected void reportDatabaseMetrics() {
        fabricProvider.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                final int tracksCount = databaseReporting.countTracks();
                fabricReporter.post(
                        Metric.create(RECORD_COUNT_METRIC, DataPoint.numeric("tracks", tracksCount)));
            }
        });
    }

    @Override
    public void handleCurrentUserChangedEvent(CurrentUserChangedEvent event) {
    }

    @Override
    public void onAppCreated(Context context) {
        pendingOnCreate.set(true);
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
        // this can theoretically happen before Fabric is initialized, so keep this check
        if (fabricProvider.isInitialized()) {
            if (shouldIncludeInCrashlyticsLogs(event)) {
                logWithCrashlytics(event);
            }
            if (event instanceof MetricEvent) {
                fabricReporter.post(((MetricEvent) event).toMetric());
            }
        }
    }

    private boolean shouldIncludeInCrashlyticsLogs(TrackingEvent event) {
        return event instanceof ScreenEvent || event instanceof UIEvent;
    }

    private void logWithCrashlytics(TrackingEvent event) {
        final CrashlyticsCore crashlytics = fabricProvider.getCrashlyticsCore();
        if (debugBuild) {
            crashlytics.log(Log.DEBUG, TAG, event.toString());
        } else {
            crashlytics.log(event.toString());
        }
    }
}
