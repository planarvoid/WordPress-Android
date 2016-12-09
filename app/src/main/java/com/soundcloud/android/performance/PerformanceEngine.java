package com.soundcloud.android.performance;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.rx.eventbus.EventBus;

import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.os.Bundle;
import android.util.Log;

public class PerformanceEngine {
    private static final String TAG = PerformanceEngine.class.getSimpleName();

    private final StopWatch stopWatch;
    private final EventBus eventBus;

    public PerformanceEngine(StopWatch stopWatch, EventBus eventBus) {
        this.stopWatch = stopWatch;
        this.eventBus = eventBus;
    }

    public void trackStartupTime(Application application) {
        application.registerActivityLifecycleCallbacks(new ActivityLifecycle(application, stopWatch));
    }

    @VisibleForTesting
    static final class ActivityLifecycle implements ActivityLifecycleCallbacks {
        private final Application application;
        private final StopWatch stopWatch;

        ActivityLifecycle(Application application, StopWatch stopWatch) {
            this.application = application;
            this.stopWatch = stopWatch;
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle bundle) {}

        @Override
        public void onActivityStarted(Activity activity) {}

        @Override
        public void onActivityResumed(Activity activity) {
            if (isApplicationMainScreen(activity)) {
                try {
                    application.unregisterActivityLifecycleCallbacks(this);
                    stopWatch.stop();
                    trackStartupTime(activity, stopWatch.getTotalTimeMillis());
                } catch (Exception exception) {
                    ErrorUtils.handleSilentException(exception);
                }
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

        private boolean isApplicationMainScreen(Activity activity) {
            return (activity instanceof MainActivity || activity instanceof OnboardActivity);
        }

        private void trackStartupTime(Activity activity, long timeMillis) {
            Log.d(TAG, String.format("Activity: %s. Startup time: %s ms.", activity.getClass().getSimpleName(), timeMillis));
        }
    }
}
