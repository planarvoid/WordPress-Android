package com.soundcloud.android.main;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.analytics.ReferringEventProvider;
import com.soundcloud.android.analytics.TrackingStateProvider;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ForegroundEvent;
import com.soundcloud.android.events.ReferringEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Intent;
import android.os.Bundle;

public class ScreenTrackerTest extends AndroidUnitTest {
    private static final ForegroundEvent EXPECTED = ForegroundEvent.open(Screen.NOTIFICATION,
                                                                         Referrer.PLAYBACK_NOTIFICATION);

    @Mock private ReferringEventProvider referringEventProvider;
    @Mock private EnterScreenDispatcher enterScreenDispatcher;
    @Mock private RootActivity activity;
    @Mock private Optional<ReferringEvent> referringEvent;
    @Mock private TrackingStateProvider trackingStateProvider;
    @Mock private FeatureFlags featureFlags;

    private ScreenTracker screenTracker;
    private TestEventBus eventBus;
    private EventTracker eventTracker;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        eventTracker = spy(new EventTracker(eventBus, trackingStateProvider, featureFlags));
        screenTracker = new ScreenTracker(referringEventProvider, enterScreenDispatcher,
                                          eventTracker);
    }

    @Test
    public void shouldTrackScreenEventWhenNotUnknown() {
        final Screen screen = Screen.ACTIVITIES;
        when(activity.getScreen()).thenReturn(screen);
        when(referringEventProvider.getReferringEvent()).thenReturn(referringEvent);
        screenTracker.onEnterScreen(activity);
        verify(eventTracker).trackScreen(any(ScreenEvent.class), eq(referringEvent));
    }

    @Test
    public void shouldNotTrackScreenEventWhenScreenIsUnknown() {
        final Screen screen = Screen.UNKNOWN;
        when(activity.getScreen()).thenReturn(screen);
        when(referringEventProvider.getReferringEvent()).thenReturn(referringEvent);
        screenTracker.onEnterScreen(activity);
        verifyZeroInteractions(eventTracker);
    }

    @Test
    public void sendForegroundEventWhenActivityStarts() {
        Intent intent = buildIntentWithReferrer();
        when(activity.getIntent()).thenReturn(intent);

        screenTracker.onCreate(activity, null);

        assertThat(getLastEventOnQueue().getAttributes()).isEqualTo(EXPECTED.getAttributes());
    }

    @Test
    public void sendForegroundEventWhenActivityReceivesNewIntent() {
        Intent intent = buildIntentWithReferrer();

        screenTracker.onNewIntent(activity, intent);

        assertThat(getLastEventOnQueue().getAttributes()).isEqualTo(EXPECTED.getAttributes());
    }

    @Test
    public void doesNotSendForegroundEventSecondTime() {
        Intent intent = buildIntentWithReferrer();
        when(activity.getIntent()).thenReturn(intent);

        screenTracker.onCreate(activity, null);
        screenTracker.onCreate(activity, null);

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(1);
    }

    @Test
    public void shouldSetupReferringEventOnCreate() {
        Intent intent = buildIntentWithReferrer();
        when(activity.getIntent()).thenReturn(intent);

        screenTracker.onCreate(activity, new Bundle());

        verify(referringEventProvider).setupReferringEvent();
    }

    @Test
    public void shouldRestoreReferringEvent() {
        final Bundle bundle = new Bundle();

        screenTracker.onRestoreInstanceState(activity, bundle);

        verify(referringEventProvider).restoreReferringEvent(bundle);
    }

    @Test
    public void shouldSaveReferringEvent() {
        final Bundle bundle = new Bundle();

        screenTracker.onSaveInstanceState(activity, bundle);

        verify(referringEventProvider).saveReferringEvent(bundle);
    }

    private Intent buildIntentWithReferrer() {
        Intent intent = new Intent();
        Referrer.PLAYBACK_NOTIFICATION.addToIntent(intent);
        Screen.NOTIFICATION.addToIntent(intent);
        return intent;
    }

    private ForegroundEvent getLastEventOnQueue() {
        return (ForegroundEvent) eventBus.lastEventOn(EventQueue.TRACKING);
    }
}
