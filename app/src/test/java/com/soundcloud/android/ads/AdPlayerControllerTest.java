package com.soundcloud.android.ads;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class AdPlayerControllerTest {
    @Mock private PlayQueueManager playQueueManager;

    private TestEventBus eventBus = new TestEventBus();
    private AdPlayerController controller;

    @Before
    public void setUp() throws Exception {
        controller = new AdPlayerController(playQueueManager, eventBus);
    }

    @Test
    public void doesNotExpandPlayerWhenAudioAdIsNotPlaying() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.forCollapsePlayer());
        setAudioAdIsPlaying(false);

        resumeFromBackground();

        expect(eventBus.lastEventOn(EventQueue.PLAYER_UI).getKind()).not.toBe(PlayerUIEvent.EXPAND_PLAYER);
    }

    @Test
    public void expandsPlayerWhenAudioAdIsPlaying() {
        setAudioAdIsPlaying(true);
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.forCollapsePlayer());

        resumeFromBackground();

        expect(eventBus.lastEventOn(EventQueue.PLAYER_UI).getKind()).toEqual(PlayerUIEvent.EXPAND_PLAYER);
    }

    @Test
    public void doesNotExpandAudioAdIfItHasAlreadyBeenExpanded() {
        setAudioAdIsPlaying(true);
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.forCollapsePlayer());

        resumeFromBackground();
        resumeFromBackground();

        expect(eventBus.eventsOn(EventQueue.PLAYER_UI)).toNumber(2); // Initialised to collapsed
        expect(eventBus.lastEventOn(EventQueue.PLAYER_UI).getKind()).toEqual(PlayerUIEvent.EXPAND_PLAYER);
    }

    @Test
    public void expandsOnlyOncePerAd() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());
        controller.onResume();
        setAudioAdIsPlaying(true);
        PlayerUIEvent playerCollapsed = PlayerUIEvent.fromPlayerCollapsed();
        eventBus.publish(EventQueue.PLAYER_UI, playerCollapsed);

        controller.onPause();
        controller.onResume();

        expect(eventBus.lastEventOn(EventQueue.PLAYER_UI)).toBe(playerCollapsed);
    }

    private void setAudioAdIsPlaying(boolean isPlaying) {
        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(isPlaying);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(Urn.forTrack(123L)));
    }

    private void resumeFromBackground() {
        controller.onResume();
        controller.onPause();
        controller.onResume();
    }
}