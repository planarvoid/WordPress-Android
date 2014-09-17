package com.soundcloud.android.ads;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.UIEvent;
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
    @Mock private AdsOperations adsOperations;

    private TestEventBus eventBus = new TestEventBus();
    private AdPlayerController controller;

    @Before
    public void setUp() throws Exception {
        controller = new AdPlayerController(eventBus, adsOperations);
    }

    @Test
    public void doesNotExpandPlayerWhenAudioAdIsNotPlaying() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());
        setAudioAdIsPlaying(false);

        resumeFromBackground();

        expect(eventBus.eventsOn(EventQueue.PLAYER_COMMAND)).toBeEmpty();
    }

    @Test
    public void expandsPlayerWhenAudioAdIsPlaying() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());
        setAudioAdIsPlaying(true);

        resumeFromBackground();

        expect(eventBus.lastEventOn(EventQueue.PLAYER_COMMAND).isExpand()).toBeTrue();
    }

    @Test
    public void emitPlayerOpenWhenAudioAdIsPlaying() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());
        setAudioAdIsPlaying(true);

        resumeFromBackground();

        UIEvent event = eventBus.lastEventOn(EventQueue.UI_TRACKING);
        UIEvent expectedEvent = UIEvent.fromPlayerOpen(UIEvent.METHOD_AD_PLAY);
        expect(event.getAttributes()).toEqual(expectedEvent.getAttributes());
    }

    @Test
    public void doesNotExpandAudioAdIfItHasAlreadyBeenExpanded() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());
        setAudioAdIsPlaying(true);

        resumeFromBackground();
        resumeFromBackground();

        expect(eventBus.eventsOn(EventQueue.PLAYER_COMMAND)).toNumber(0);
    }

    @Test
    public void doNotEmitPlayerOpenIfItHasAlreadyBeenExpanded() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());
        setAudioAdIsPlaying(true);

        resumeFromBackground();
        resumeFromBackground();

        eventBus.verifyNoEventsOn(EventQueue.UI_TRACKING);
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
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(isPlaying);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(Urn.forTrack(123L)));
    }

    private void resumeFromBackground() {
        controller.onResume();
        controller.onPause();
        controller.onResume();
    }
}