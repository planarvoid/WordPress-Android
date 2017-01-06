package com.soundcloud.android.performance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.discovery.SearchActivity;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PerformanceEvent;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.performance.PerformanceEngine.ActivityLifecycle;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;

public class PerformanceEngineTest extends AndroidUnitTest {

    private PerformanceEngine performanceEngine;
    private ActivityLifecycle activityLifecycle;

    @Mock private StopWatch stopWatch;
    @Mock private Application application;
    @Mock private DeviceHelper deviceHelper;

    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() {
        performanceEngine = new PerformanceEngine(stopWatch, eventBus, deviceHelper);
        activityLifecycle = new ActivityLifecycle(application, stopWatch, eventBus, deviceHelper);
        when(deviceHelper.getAppVersionName()).thenReturn("beta");
        when(deviceHelper.getDeviceName()).thenReturn("nexus");
        when(deviceHelper.getAndroidReleaseVersion()).thenReturn("lollipop");
    }

    @Test
    public void shouldRegisterActivityCallbacks() {
        performanceEngine.trackStartupTime(application);

        verify(application).registerActivityLifecycleCallbacks(any(ActivityLifecycle.class));
    }

    @Test
    public void shouldStopStopWatchAndUnregisterCallbacksOnResumeMainActivity() {
        final InOrder inOrder = inOrder(application, stopWatch);

        activityLifecycle.onActivityResumed(mockActivity(MainActivity.class));

        inOrder.verify(application).unregisterActivityLifecycleCallbacks(activityLifecycle);
        inOrder.verify(stopWatch).stop();

        assertThat(eventBus.eventsOn(EventQueue.PERFORMANCE)).hasSize(1);
        assertThat(eventBus.lastEventOn(EventQueue.PERFORMANCE).name()).isEqualTo(PerformanceEvent.METRIC_APP_STARTUP_TIME);
    }

    @Test
    public void shouldStopStopWatchAndUnregisterCallbacksOnResumeOnboardActivity() {
        final InOrder inOrder = inOrder(application, stopWatch);

        activityLifecycle.onActivityResumed(mockActivity(OnboardActivity.class));

        inOrder.verify(application).unregisterActivityLifecycleCallbacks(activityLifecycle);
        inOrder.verify(stopWatch).stop();

        assertThat(eventBus.eventsOn(EventQueue.PERFORMANCE)).hasSize(1);
        assertThat(eventBus.lastEventOn(EventQueue.PERFORMANCE).name()).isEqualTo(PerformanceEvent.METRIC_APP_STARTUP_TIME);
    }

    @Test
    public void shouldNotMeasureIfIsNotMainScreen() {
        activityLifecycle.onActivityResumed(mockActivity(SearchActivity.class));

        verifyZeroInteractions(application, stopWatch);
    }

    private Activity mockActivity(Class<? extends Activity> activityClass) {
        final Activity activity = mock(Activity.class);
        final ComponentName componentName = mock(ComponentName.class);
        when(componentName.getClassName()).thenReturn(activityClass.getName());
        when(activity.getComponentName()).thenReturn(componentName);

        return activity;
    }
}
