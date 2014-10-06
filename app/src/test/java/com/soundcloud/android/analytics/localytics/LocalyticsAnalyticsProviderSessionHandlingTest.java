package com.soundcloud.android.analytics.localytics;


import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.TestEvents;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.app.Activity;

@RunWith(SoundCloudTestRunner.class)
public class LocalyticsAnalyticsProviderSessionHandlingTest {

    private LocalyticsAnalyticsProvider localyticsProvider;

    @Mock private LocalyticsSession localyticsSession;
    @Mock private PlaybackStateProvider playbackWrapper;

    @Before
    public void setUp() throws Exception {
        localyticsProvider = new LocalyticsAnalyticsProvider(localyticsSession, playbackWrapper);
    }

    @After
    public void tearDown(){
        LocalyticsAnalyticsProvider.ACTIVITY_SESSION_OPEN.set(false);
    }

    @Test
    public void shouldOpenSessionForActivityLifeCycleCreateEvent(){
        localyticsProvider.handleActivityLifeCycleEvent(ActivityLifeCycleEvent.forOnCreate(Activity.class));
        verify(localyticsSession).open();
    }

    @Test
    public void shouldOpenSessionForActivityLifeCycleResumeEvent(){
        localyticsProvider.handleActivityLifeCycleEvent(ActivityLifeCycleEvent.forOnResume(Activity.class));
        verify(localyticsSession).open();
    }

    @Test
    public void shouldCloseSessionForActivityLifeCyclePauseEvent(){
        localyticsProvider.handleActivityLifeCycleEvent(ActivityLifeCycleEvent.forOnPause(Activity.class));
        verify(localyticsSession).close();
    }

    @Test
    public void shouldCallCloseSessionWhenActivityPausedAndPlayerNotPlaying(){
        when(playbackWrapper.isSupposedToBePlaying()).thenReturn(false);

        localyticsProvider.handleActivityLifeCycleEvent(ActivityLifeCycleEvent.forOnPause(Activity.class));

        verify(localyticsSession).close();

    }

    @Test
    public void shouldNotCallCloseSessionIfActivityIsPausedAndPlayerIsPlaying(){
        when(playbackWrapper.isSupposedToBePlaying()).thenReturn(true);

        localyticsProvider.handleActivityLifeCycleEvent(ActivityLifeCycleEvent.forOnPause(Activity.class));

        verify(localyticsSession, never()).close();
    }


    @Test
    public void shouldSetTheActivitySessionStateToTrueWhenOpeningActivitySession(){
        localyticsProvider.handleActivityLifeCycleEvent(ActivityLifeCycleEvent.forOnCreate(Activity.class));

        expect(localyticsProvider.isActivitySessionClosed()).toBeFalse();
    }

    @Test
    public void shouldSetTheActivitySessionStateToFalseWhenClosingActivitySession(){
        localyticsProvider.handleActivityLifeCycleEvent(ActivityLifeCycleEvent.forOnPause(Activity.class));

        expect(localyticsProvider.isActivitySessionClosed()).toBeTrue();
    }

    @Test
    public void shouldAlwaysOpenSessionWhenReceivingStopPlaybackEvent() throws Exception {
        localyticsProvider.handleTrackingEvent(TestEvents.playbackSessionStopEvent());
        verify(localyticsSession).open();
    }
}
