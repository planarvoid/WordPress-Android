package com.soundcloud.android.ads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.support.v7.app.AppCompatActivity;

public class AdPlayerControllerTest extends AndroidUnitTest {

    @Mock private PlayQueueManager playQueueManager;
    @Mock private AdsOperations adsOperations;
    @Mock private AppCompatActivity activity;

    private TestEventBus eventBus = new TestEventBus();
    private AdPlayerController controller;

    @Before
    public void setUp() throws Exception {
        controller = new AdPlayerController(eventBus, adsOperations);
    }

    @Test
    public void unlocksPlayerWhenRegularTrackIsPlaying() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());
        setAudioAdIsPlaying(false);

        resumeFromBackground();

        assertThat(eventBus.lastEventOn(EventQueue.PLAYER_COMMAND).isUnlock()).isTrue();
    }

    @Test
    public void expandsAndUnlocksPlayerWhenAudioAdIsPlaying() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());
        setAudioAdIsPlaying(true);

        resumeFromBackground();

        assertThat(eventBus.eventsOn(EventQueue.PLAYER_COMMAND)).contains(PlayerUICommand.unlockPlayer());
        assertThat(eventBus.eventsOn(EventQueue.PLAYER_COMMAND)).contains(PlayerUICommand.expandPlayer());
    }

    @Test
    public void locksPlayerWhenVideoAdIsPlaying() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());
        setVideoAdIsPlaying();

        resumeFromBackground();
        assertThat(eventBus.lastEventOn(EventQueue.PLAYER_COMMAND).isLockExpanded()).isTrue();
    }

    @Test
    public void emitPlayerOpenWhenAudioAdIsPlaying() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());
        setAudioAdIsPlaying(true);

        resumeFromBackground();

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        UIEvent expectedEvent = UIEvent.fromPlayerOpen(UIEvent.METHOD_AD_PLAY);
        assertThat(event.getAttributes()).isEqualTo(expectedEvent.getAttributes());
    }

    @Test
    public void doesNotExpandAudioAdIfItHasAlreadyBeenExpanded() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());
        setAudioAdIsPlaying(true);

        resumeFromBackground();
        resumeFromBackground();

        assertThat(eventBus.eventsOn(EventQueue.PLAYER_COMMAND)).doesNotContain(PlayerUICommand.expandPlayer());
    }

    @Test
    public void doNotEmitPlayerOpenIfItHasAlreadyBeenExpanded() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());
        setAudioAdIsPlaying(true);

        resumeFromBackground();
        resumeFromBackground();

        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void expandsOnlyOncePerAd() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());
        controller.onResume(activity);
        setAudioAdIsPlaying(true);
        PlayerUIEvent playerCollapsed = PlayerUIEvent.fromPlayerCollapsed();
        eventBus.publish(EventQueue.PLAYER_UI, playerCollapsed);

        controller.onPause(activity);
        controller.onResume(activity);

        assertThat(eventBus.lastEventOn(EventQueue.PLAYER_UI)).isSameAs(playerCollapsed);
    }

    private void setAudioAdIsPlaying(boolean isPlaying) {
        when(adsOperations.isCurrentItemAd()).thenReturn(isPlaying);
        when(adsOperations.isCurrentItemAudioAd()).thenReturn(isPlaying);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(Urn.forTrack(123L)), Urn.NOT_SET, 0));
    }

    private void setVideoAdIsPlaying() {
        when(adsOperations.isCurrentItemAd()).thenReturn(true);
        when(adsOperations.isCurrentItemVideoAd()).thenReturn(true);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createVideo(AdFixtures.getVideoAd(Urn.forTrack(123L))), Urn.NOT_SET, 0));
    }

    private void resumeFromBackground() {
        controller.onResume(activity);
        controller.onPause(activity);
        controller.onResume(activity);
    }
}
