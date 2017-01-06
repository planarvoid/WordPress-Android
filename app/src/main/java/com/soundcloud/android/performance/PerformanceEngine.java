package com.soundcloud.android.performance;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PerformanceEvent;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.rx.eventbus.EventBus;

import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.os.Bundle;

@AutoFactory(allowSubclasses = true)
public class PerformanceEngine {
    private static final String TAG = PerformanceEngine.class.getSimpleName();

    private final StopWatch stopWatch;
    private final EventBus eventBus;
    private final DeviceHelper deviceHelper;

    PerformanceEngine(StopWatch stopWatch, @Provided EventBus eventBus, @Provided DeviceHelper deviceHelper) {
        this.stopWatch = stopWatch;
        this.eventBus = eventBus;
        this.deviceHelper = deviceHelper;
    }

    public void trackStartupTime(Application application) {
        application.registerActivityLifecycleCallbacks(new ActivityLifecycle(application, stopWatch, eventBus,
                deviceHelper));
    }

    @VisibleForTesting
    static final class ActivityLifecycle implements ActivityLifecycleCallbacks {
        private final Application application;
        private final StopWatch stopWatch;
        private final EventBus eventBus;
        private final DeviceHelper deviceHelper;

        ActivityLifecycle(Application application, StopWatch stopWatch, EventBus eventBus, DeviceHelper deviceHelper) {
            this.application = application;
            this.stopWatch = stopWatch;
            this.eventBus = eventBus;
            this.deviceHelper = deviceHelper;
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle bundle) {}

        @Override
        public void onActivityStarted(Activity activity) {}

        @Override
        public void onActivityResumed(Activity activity) {
            try {
                final UiLatencyMetric uiLatencyMetric = UiLatencyMetric.fromActivity(activity);
                if (uiLatencyMetric != UiLatencyMetric.NONE) {
                    application.unregisterActivityLifecycleCallbacks(this);
                    stopWatch.stop();
                    trackApplicationStartupTime(uiLatencyMetric);
                }
            } catch (Exception exception) {
                ErrorUtils.handleSilentException(exception);
            }
        }

        @Override
        public void onActivityPaused(Activity activity) {}

        @Override
        public void onActivityStopped(Activity activity) {}

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {}

        @Override
        public void onActivityDestroyed(Activity activity) {}

        private void trackApplicationStartupTime(UiLatencyMetric uiLatencyMetric) {
            final PerformanceEvent performanceEvent = PerformanceEvent.forApplicationStartupTime(
                    uiLatencyMetric == UiLatencyMetric.MAIN_AUTHENTICATED, stopWatch.getTotalTimeMillis(),
                    deviceHelper.getAppVersionName(), deviceHelper.getDeviceName(),
                    deviceHelper.getAndroidReleaseVersion());
            eventBus.publish(EventQueue.PERFORMANCE, performanceEvent);
        }
    }
}
