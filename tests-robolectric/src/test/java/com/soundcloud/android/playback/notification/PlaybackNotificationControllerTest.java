package com.soundcloud.android.playback.notification;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class PlaybackNotificationControllerTest {

    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private PlaybackNotificationController controller;
    private TestEventBus eventBus = new TestEventBus();
    @Mock private BackgroundPlaybackNotificationController backgroundController;
    @Mock private ForegroundPlaybackNotificationController foregroundController;

    @Before
    public void setUp() throws Exception {
        controller = new PlaybackNotificationController(eventBus, backgroundController, foregroundController, 0);
    }

    @Test
    public void playQueueEventDoesNotCreateNotificationBeforePlaybackServiceCreated() {
        controller.subscribe();
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(TRACK_URN));

        verify(backgroundController, never()).setTrack(any(PropertySet.class));
    }

    @Test
    public void playQueueEventDoesNotCreateNotificationAfterPlaybackServiceDestroyed() {
        controller.subscribe();
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        reset(backgroundController);
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forDestroyed());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(TRACK_URN));

        verify(backgroundController, never()).setTrack(any(PropertySet.class));
    }

    @Test
    public void playQueueEventUpdatesNotificationAfterPlaybackServiceWasStoppedAndStartedAgain() {
        final CurrentPlayQueueTrackEvent event = CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN);

        controller.subscribe();
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forStopped());
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forStarted());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, event);

        verify(backgroundController).setTrack(event.getCurrentMetaData().put(EntityProperty.URN, event.getCurrentTrackUrn()));
    }

    @Test
    public void serviceDestroyedEventCancelsAnyCurrentNotification() {
        controller.subscribe();

        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forDestroyed());

        verify(backgroundController).clear();
    }

    @Test
    public void playQueueEventCreatesNewNotificationFromNewPlayQueueEvent() {
        final CurrentPlayQueueTrackEvent event = CurrentPlayQueueTrackEvent.fromNewQueue(TRACK_URN);

        controller.subscribe();
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, event);

        verify(backgroundController).setTrack(event.getCurrentMetaData().put(EntityProperty.URN, event.getCurrentTrackUrn()));
    }

    @Test
    public void playQueueEventDoesNotCreateNewNotificationFromQueueUpdateEvent() {
        controller.subscribe();
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromNewQueue(TRACK_URN));

        verify(backgroundController, never()).setTrack(any(PropertySet.class));
    }

    @Test
    public void playQueueEventDoesNotifiesAgainAfterBitmapLoadedIfServiceNotCreated() {
        controller.subscribe();
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN));

        verify(backgroundController, never()).setTrack(any(PropertySet.class));
    }

    @Test
    public void playQueueEventDoesNotifiesAgainAfterBitmapLoadedIfServiceDestroyed() {
        controller.subscribe();
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        reset(backgroundController);
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forDestroyed());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN));

        verify(backgroundController, never()).setTrack(any(PropertySet.class));
    }

    @Test
    public void usesBackgroundDelegateInBackground() {
        final CurrentPlayQueueTrackEvent event = CurrentPlayQueueTrackEvent.fromNewQueue(TRACK_URN);
        controller.subscribe();
        controller.onPause(null);

        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, event);

        verify(foregroundController, never()).setTrack(any(PropertySet.class));
        verify(backgroundController).setTrack(event.getCurrentMetaData().put(EntityProperty.URN, event.getCurrentTrackUrn()));
    }

    @Test
    public void usesForegroundDelegateWhenInForeground() {
        final CurrentPlayQueueTrackEvent event = CurrentPlayQueueTrackEvent.fromNewQueue(TRACK_URN);
        controller.subscribe();
        controller.onResume(null);

        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, event);

        verify(backgroundController, never()).setTrack(any(PropertySet.class));
        verify(foregroundController).setTrack(event.getCurrentMetaData().put(EntityProperty.URN, event.getCurrentTrackUrn()));
    }

    @Test
    public void setTrackWhenSwitchingController() {
        final CurrentPlayQueueTrackEvent event = CurrentPlayQueueTrackEvent.fromNewQueue(TRACK_URN);
        controller.subscribe();

        controller.onResume(null);
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forStarted());
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, event);
        controller.onPause(null);

        verify(backgroundController).setTrack(any(PropertySet.class));
    }
}
