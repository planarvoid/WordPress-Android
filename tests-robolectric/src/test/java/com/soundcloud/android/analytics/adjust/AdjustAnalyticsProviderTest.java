package com.soundcloud.android.analytics.adjust;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.app.Activity;

@RunWith(SoundCloudTestRunner.class)
public class AdjustAnalyticsProviderTest {
    @Mock private AdjustWrapper adjustWrapper;
    private AdjustAnalyticsProvider adjustAnalyticsProvider;

    @Before
    public void setUp() throws Exception {
        adjustAnalyticsProvider = new AdjustAnalyticsProvider(adjustWrapper);
    }

    @Test
    public void shouldTrackOnResume() {
        ActivityLifeCycleEvent event = ActivityLifeCycleEvent.forOnResume(Activity.class);
        adjustAnalyticsProvider.handleActivityLifeCycleEvent(event);
        verify(adjustWrapper).onResume();
    }

    @Test
    public void shouldTrackOnPause() {
        ActivityLifeCycleEvent event = ActivityLifeCycleEvent.forOnPause(Activity.class);
        adjustAnalyticsProvider.handleActivityLifeCycleEvent(event);
        verify(adjustWrapper).onPause();
    }
}