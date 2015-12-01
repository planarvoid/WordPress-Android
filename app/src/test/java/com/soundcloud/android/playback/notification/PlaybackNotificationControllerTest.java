package com.soundcloud.android.playback.notification;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.soundcloud.android.ads.AdFunctions;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlaybackService;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class PlaybackNotificationControllerTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private static final PlayQueueItem TRACK_PLAY_QUEUE_ITEM = TestPlayQueueItem.createTrack(TRACK_URN);
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
        final CurrentPlayQueueItemEvent event = CurrentPlayQueueItemEvent.fromPositionChanged(TRACK_PLAY_QUEUE_ITEM, Urn.NOT_SET, 0);

        controller.subscribe(playbackService);
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forStopped());
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forStarted());
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, event);

        verify(backgroundController).setTrack(playbackService, getTrackMetadata(event));
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

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(TRACK_PLAY_QUEUE_ITEM, Urn.NOT_SET, 0));

        verifyZeroInteractions(backgroundController);
    }

    @Test
    public void playQueueEventCreatesNewNotificationFromNewPlayQueueEvent() {
        final CurrentPlayQueueItemEvent event = CurrentPlayQueueItemEvent.fromNewQueue(TRACK_PLAY_QUEUE_ITEM, Urn.NOT_SET, 0);

        controller.subscribe(playbackService);
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, event);

        verify(backgroundController).setTrack(playbackService, getTrackMetadata(event));
    }

    @Test
    public void usesBackgroundDelegateInBackground() {
        final CurrentPlayQueueItemEvent event = CurrentPlayQueueItemEvent.fromNewQueue(TRACK_PLAY_QUEUE_ITEM, Urn.NOT_SET, 0);
        controller.subscribe(playbackService);
        controller.onPause(null);

        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, event);

        verify(foregroundController, never()).setTrack(any(PlaybackService.class), any(PropertySet.class));
        verify(backgroundController).setTrack(playbackService, getTrackMetadata(event));
    }

    @Test
    public void usesForegroundDelegateWhenInForeground() {
        final CurrentPlayQueueItemEvent event = CurrentPlayQueueItemEvent.fromNewQueue(TRACK_PLAY_QUEUE_ITEM, Urn.NOT_SET, 0);
        controller.subscribe(playbackService);
        controller.onResume(null);

        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forCreated());
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, event);

        verify(backgroundController, never()).setTrack(any(PlaybackService.class), any(PropertySet.class));
        verify(foregroundController).setTrack(playbackService, getTrackMetadata(event));
    }

    @Test
    public void setTrackWhenSwitchingController() {
        final CurrentPlayQueueItemEvent event = CurrentPlayQueueItemEvent.fromNewQueue(TRACK_PLAY_QUEUE_ITEM, Urn.NOT_SET, 0);
        controller.subscribe(playbackService);

        controller.onResume(null);
        eventBus.publish(EventQueue.PLAYER_LIFE_CYCLE, PlayerLifeCycleEvent.forStarted());
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, event);
        controller.onPause(null);

        verify(backgroundController).setTrack(same(playbackService), any(PropertySet.class));
    }

    @Test
    public void unsubscribeNoOpWhenNotSubscribed() {
        controller.unsubscribe();

        verifyZeroInteractions(backgroundController);
    }

    private PropertySet getTrackMetadata(CurrentPlayQueueItemEvent event) {
        final TrackQueueItem playQueueItem = (TrackQueueItem) event.getCurrentPlayQueueItem();
        final boolean isAd = AdFunctions.IS_AUDIO_AD_ITEM.apply(event.getCurrentPlayQueueItem());
        return PropertySet.from(AdProperty.IS_AUDIO_AD.bind(isAd)).put(EntityProperty.URN, playQueueItem.getTrackUrn());
    }

}
