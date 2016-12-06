package com.soundcloud.android.performance;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.performance.PerformanceEngine.ActivityLifecycle;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;

import android.app.Activity;
import android.app.Application;

public class PerformanceEngineTest extends AndroidUnitTest {

    private PerformanceEngine performanceEngine;
    private ActivityLifecycle activityLifecycle;

    @Mock private StopWatch stopWatch;
    @Mock private Application application;

    @Before
    public void setUp() {
        performanceEngine = new PerformanceEngine(stopWatch);
        activityLifecycle = new ActivityLifecycle(application, stopWatch);
    }

    @Test
    public void shouldStartStopWatchAndRegisterActivityCallbacks() {
        final InOrder inOrder = inOrder(stopWatch, application);

        performanceEngine.trackStartupTime(application);

        inOrder.verify(stopWatch).start();
        inOrder.verify(application).registerActivityLifecycleCallbacks(any(ActivityLifecycle.class));
    }

    @Test
    public void shouldStopStopWatchAndUnregisterCallbacksOnResumeMainActivity() {
        final MainActivity mainActivity = mock(MainActivity.class);
        final InOrder inOrder = inOrder(application, stopWatch);

        activityLifecycle.onActivityResumed(mainActivity);

        inOrder.verify(application).unregisterActivityLifecycleCallbacks(activityLifecycle);
        inOrder.verify(stopWatch).stop();
    }

    @Test
    public void shouldStopStopWatchAndUnregisterCallbacksOnResumeOnboardActivity() {
        final OnboardActivity onboardActivity = mock(OnboardActivity.class);
        final InOrder inOrder = inOrder(application, stopWatch);

        activityLifecycle.onActivityResumed(onboardActivity);

        inOrder.verify(application).unregisterActivityLifecycleCallbacks(activityLifecycle);
        inOrder.verify(stopWatch).stop();
    }

    @Test
    public void shouldNotMeasureIfIsNotMainScreen() {
        final Activity activity = mock(Activity.class);

        activityLifecycle.onActivityResumed(activity);

        verifyZeroInteractions(application, stopWatch);
    }
}
