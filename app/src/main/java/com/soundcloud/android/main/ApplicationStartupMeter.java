package com.soundcloud.android.main;


import static com.soundcloud.android.analytics.performance.MetricKey.USER_LOGGED_IN;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.analytics.performance.MetricParams;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetric;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.java.optional.Optional;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

@AutoFactory
public final class ApplicationStartupMeter implements Application.ActivityLifecycleCallbacks {

    private final PerformanceMetricsEngine performanceMetricsEngine;
    private final Application application;

    ApplicationStartupMeter(Application application, @Provided PerformanceMetricsEngine performanceMetricsEngine) {
        this.application = application;
        this.performanceMetricsEngine = performanceMetricsEngine;
    }

    public void subscribe() {
        application.registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        if (isLauncherActivity(activity)) {
            performanceMetricsEngine.startMeasuring(PerformanceMetric.create(MetricType.APP_UI_VISIBLE));
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
        try {
            Optional<Boolean> userLoggedIn = isUserLoggedIn(activity);

            if (userLoggedIn.isPresent()) {
                MetricParams metricParams = new MetricParams().putBoolean(USER_LOGGED_IN, userLoggedIn.get());

                application.unregisterActivityLifecycleCallbacks(this);
                performanceMetricsEngine.endMeasuring(PerformanceMetric.builder()
                                                              .metricType(MetricType.APP_UI_VISIBLE)
                                                              .metricParams(metricParams)
                                                              .build());
            }
        } catch (Exception exception) {
            ErrorUtils.handleSilentException(exception);
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }

    private Optional<Boolean> isUserLoggedIn(Activity activity) {
        if (activity instanceof MainActivity) {
            return Optional.of(true);
        } else if (activity instanceof OnboardActivity) {
            return Optional.of(false);
        } else {
            return Optional.absent();
        }
    }

    private boolean isLauncherActivity(Activity activity) {
        return activity instanceof LauncherActivity;
    }
}
