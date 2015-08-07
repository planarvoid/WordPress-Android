package com.soundcloud.android.playback.notification;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackService;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class PlaybackNotificationControllerTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private PlaybackNotificationController controller;
    private TestEventBus eventBus = new TestEventBus();
    @Mock private BackgroundPlaybackNotificationController backgroundController;
    @Mock private ForegroundPlaybackNotificationController foregroundController;
    @Mock private PlaybackService playbackService;

    @Before
    public void setUp() throws Exception {
        controller = new PlaybackNotificationController(eventBus, backgroundController, foregroundController, 0);
    }

    @Test
    public void playQueueEventUpdatesNotificationAfterPlaybackServiceWasStoppedAndStartedAgain() {
        final CurrentPlayQueueTrackEvent event = CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN, Urn.NOT_SET, 0);

        controller.subscribe(playbackService);
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forStopped());
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forStarted());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, event);

        verify(backgroundController).setTrack(event.getCurrentMetaData().put(EntityProperty.URN, event.getCurrentTrackUrn()));
    }

    @Test
    public void unsubscribeCancelsAnyCurrentNotification() {
        controller.subscribe(playbackService);
        controller.unsubscribe();


        verify(backgroundController).clear(playbackService);
    }

    @Test
    public void unsubscribeShouldClearNotifications() {
        controller.subscribe(playbackService);
        reset(backgroundController);

        controller.unsubscribe();

        verify(backgroundController).clear(playbackService);
    }

    @Test
    public void unsubscribeShouldDisableNotifications() {
        controller.subscribe(playbackService);
        controller.unsubscribe();
        reset(backgroundController);

        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN, Urn.NOT_SET, 0));

        verifyZeroInteractions(backgroundController);
    }

    @Test
    public void playQueueEventCreatesNewNotificationFromNewPlayQueueEvent() {
        final CurrentPlayQueueTrackEvent event = CurrentPlayQueueTrackEvent.fromNewQueue(TRACK_URN, Urn.NOT_SET, 0);

        controller.subscribe(playbackService);
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, event);

        verify(backgroundController).setTrack(event.getCurrentMetaData().put(EntityProperty.URN, event.getCurrentTrackUrn()));
    }

    @Test
    public void usesBackgroundDelegateInBackground() {
        final CurrentPlayQueueTrackEvent event = CurrentPlayQueueTrackEvent.fromNewQueue(TRACK_URN, Urn.NOT_SET, 0);
        controller.subscribe(playbackService);
        controller.onPause(null);

        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, event);

        verify(foregroundController, never()).setTrack(any(PropertySet.class));
        verify(backgroundController).setTrack(event.getCurrentMetaData().put(EntityProperty.URN, event.getCurrentTrackUrn()));
    }

    @Test
    public void usesForegroundDelegateWhenInForeground() {
        final CurrentPlayQueueTrackEvent event = CurrentPlayQueueTrackEvent.fromNewQueue(TRACK_URN, Urn.NOT_SET, 0);
        controller.subscribe(playbackService);
        controller.onResume(null);

        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, event);

        verify(backgroundController, never()).setTrack(any(PropertySet.class));
        verify(foregroundController).setTrack(event.getCurrentMetaData().put(EntityProperty.URN, event.getCurrentTrackUrn()));
    }

    @Test
    public void setTrackWhenSwitchingController() {
        final CurrentPlayQueueTrackEvent event = CurrentPlayQueueTrackEvent.fromNewQueue(TRACK_URN, Urn.NOT_SET, 0);
        controller.subscribe(playbackService);

        controller.onResume(null);
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forStarted());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, event);
        controller.onPause(null);

        verify(backgroundController).setTrack(any(PropertySet.class));
    }

    @Test
    public void unsubscribeNoOpWhenNotSubscribed() {
        controller.unsubscribe();

        verifyZeroInteractions(backgroundController);
    }
}
