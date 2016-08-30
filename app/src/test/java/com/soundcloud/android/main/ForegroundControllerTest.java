package com.soundcloud.android.main;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.Referrer;
import com.soundcloud.android.analytics.TrackingStateProvider;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ForegroundEvent;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

public class ForegroundControllerTest extends AndroidUnitTest {
    private static final ForegroundEvent EXPECTED = ForegroundEvent.open(Screen.NOTIFICATION,
                                                                         Referrer.PLAYBACK_NOTIFICATION);

    @Mock private AppCompatActivity activity;
    @Mock private TrackingStateProvider trackingStateProvider;
    @Mock private FeatureFlags featureFlags;

    private TestEventBus eventBus;
    private ForegroundController lightCycle;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        EventTracker eventTracker = new EventTracker(eventBus, trackingStateProvider, featureFlags);
        lightCycle = new ForegroundController(eventTracker);
    }

    @Test
    public void sendForegroundEventWhenActivityStarts() {
        Intent intent = buildIntentWithReferrer();
        when(activity.getIntent()).thenReturn(intent);

        lightCycle.onCreate(activity, null);

        assertThat(getLastEventOnQueue().getAttributes()).isEqualTo(EXPECTED.getAttributes());
    }

    @Test
    public void sendForegroundEventWhenActivityReceivesNewIntent() {
        Intent intent = buildIntentWithReferrer();

        lightCycle.onNewIntent(activity, intent);

        assertThat(getLastEventOnQueue().getAttributes()).isEqualTo(EXPECTED.getAttributes());
    }

    @Test
    public void doesNotSendForegroundEventSecondTime() {
        Intent intent = buildIntentWithReferrer();
        when(activity.getIntent()).thenReturn(intent);

        lightCycle.onCreate(activity, null);
        lightCycle.onCreate(activity, null);

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(1);
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
