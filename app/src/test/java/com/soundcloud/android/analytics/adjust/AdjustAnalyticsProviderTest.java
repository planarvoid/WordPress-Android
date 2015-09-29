package com.soundcloud.android.analytics.adjust;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.app.Activity;

public class AdjustAnalyticsProviderTest extends AndroidUnitTest {
    @Mock private AdjustWrapper adjustWrapper;
    @Mock private Activity activity;
    private AdjustAnalyticsProvider adjustAnalyticsProvider;

    @Before
    public void setUp() throws Exception {
        adjustAnalyticsProvider = new AdjustAnalyticsProvider(adjustWrapper);
    }

    @Test
    public void shouldTrackOnResume() {
        ActivityLifeCycleEvent event = ActivityLifeCycleEvent.forOnResume(activity);
        adjustAnalyticsProvider.handleActivityLifeCycleEvent(event);
        verify(adjustWrapper).onResume();
    }

    @Test
    public void shouldTrackOnPause() {
        ActivityLifeCycleEvent event = ActivityLifeCycleEvent.forOnPause(activity);
        adjustAnalyticsProvider.handleActivityLifeCycleEvent(event);
        verify(adjustWrapper).onPause();
    }
}