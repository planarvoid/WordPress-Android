package com.soundcloud.android.ads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.VideoAdQueueItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.support.v7.app.AppCompatActivity;

public class AdPlayerControllerTest extends AndroidUnitTest {

    public static final Urn TRACK_URN = Urn.forTrack(123L);

    @Mock private PlaySessionController playSessionController;
    @Mock private AdsOperations adsOperations;
    @Mock private AppCompatActivity activity;

    private TestEventBus eventBus = new TestEventBus();
    private AdPlayerController controller;

    @Before
    public void setUp() throws Exception {
        controller = new AdPlayerController(eventBus, adsOperations, playSessionController);
    }

    @Test
    public void expandsAndUnlocksPlayerWhenAudioAdIsPlaying() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());
        setAudioPlaying(AdFixtures.getAudioAd(TRACK_URN));

        resumeFromBackground();

        assertThat(eventBus.eventsOn(EventQueue.PLAYER_COMMAND)).contains(PlayerUICommand.unlockPlayer());
        assertThat(eventBus.eventsOn(EventQueue.PLAYER_COMMAND)).contains(PlayerUICommand.expandPlayer());
    }

    @Test
    public void doesNotExpandPlayerIfCompanionlessAudioAdIsPlaying() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());
        setAudioPlaying(AdFixtures.getCompanionlessAudioAd(TRACK_URN));

        resumeFromBackground();

        assertThat(eventBus.eventsOn(EventQueue.PLAYER_COMMAND)).contains(PlayerUICommand.unlockPlayer());
        assertThat(eventBus.eventsOn(EventQueue.PLAYER_COMMAND)).doesNotContain(PlayerUICommand.expandPlayer());
    }

    @Test
    public void locksPlayerWhenVideoAdIsPlaying() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());
        setVideoAdIsPlaying(true);

        resumeFromBackground();
        assertThat(eventBus.lastEventOn(EventQueue.PLAYER_COMMAND).isLockExpanded()).isTrue();
    }

    @Test
    public void hidesPlayQueueWhenVideoAd() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());
        setVideoAdIsPlaying(true);

        resumeFromBackground();
        assertThat(eventBus.lastEventOn(EventQueue.PLAY_QUEUE_UI).isHideEvent()).isTrue();
    }

    @Test
    public void hidesPlayQueueWhenAudioAd() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());
        setAudioPlaying(AdFixtures.getAudioAd(TRACK_URN));

        resumeFromBackground();
        assertThat(eventBus.lastEventOn(EventQueue.PLAY_QUEUE_UI).isHideEvent()).isTrue();
    }

    @Test
    public void emitPlayerOpenWhenAudioAdIsPlaying() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());
        setAudioPlaying(AdFixtures.getAudioAd(TRACK_URN));

        resumeFromBackground();

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        UIEvent expectedEvent = UIEvent.fromPlayerOpen();
        assertThat(event.getAttributes()).isEqualTo(expectedEvent.getAttributes());
    }

    @Test
    public void doesNotExpandAudioAdIfItHasAlreadyBeenExpanded() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());
        setAudioPlaying(AdFixtures.getAudioAd(TRACK_URN));

        resumeFromBackground();
        resumeFromBackground();

        assertThat(eventBus.eventsOn(EventQueue.PLAYER_COMMAND)).doesNotContain(PlayerUICommand.expandPlayer());
    }

    @Test
    public void doNotEmitPlayerOpenIfItHasAlreadyBeenExpanded() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());
        setAudioPlaying(AdFixtures.getAudioAd(TRACK_URN));

        resumeFromBackground();
        resumeFromBackground();

        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
    }

    @Test
    public void expandsOnlyOncePerAd() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());
        controller.onResume(activity);
        setAudioPlaying(AdFixtures.getAudioAd(TRACK_URN));
        PlayerUIEvent playerCollapsed = PlayerUIEvent.fromPlayerCollapsed();
        eventBus.publish(EventQueue.PLAYER_UI, playerCollapsed);

        controller.onPause(activity);
        controller.onResume(activity);

        assertThat(eventBus.lastEventOn(EventQueue.PLAYER_UI)).isSameAs(playerCollapsed);
    }

    @Test
    public void videoAdsPausedWhenAppInBackground() {
        when(activity.isChangingConfigurations()).thenReturn(false);
        setVideoAdIsPlaying(true);

        controller.onPause(activity);

        verify(playSessionController).pause();
    }

    @Test
    public void videoAdsNotPausedWhenAppChangingOrientations() {
        when(activity.isChangingConfigurations()).thenReturn(true);
        setVideoAdIsPlaying(true);

        controller.onPause(activity);

        verify(playSessionController, never()).pause();
    }

    @Test
    public void unlocksPlayerAndResetOrientationToUnspecifiedWhenRegularTrackIsPlaying() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());
        setAudioPlaying();

        controller.onResume(activity);

        assertThat(eventBus.lastEventOn(EventQueue.PLAYER_COMMAND).isUnlock()).isTrue();
    }

    private void setAudioPlaying() {
        setAudioPlaying(null);
    }

    private void setAudioPlaying(AdData adData) {
        boolean isAd = adData != null;
        final PlayQueueItem item = isAd
                ? TestPlayQueueItem.createAudioAd((AudioAd) adData)
                : TestPlayQueueItem.createTrack(TRACK_URN);

        when(adsOperations.isCurrentItemAd()).thenReturn(isAd);
        when(adsOperations.isCurrentItemAudioAd()).thenReturn(isAd);
        when(adsOperations.getCurrentTrackAdData()).thenReturn(item.getAdData());
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(item, Urn.NOT_SET, 0));
    }

    private void setVideoAdIsPlaying(boolean verticalVideo) {
        final ApiVideoSource videoSource = verticalVideo ?
                                           AdFixtures.getApiVideoSource(300, 600) :
                                           AdFixtures.getApiVideoSource(600, 300);
        final VideoAdQueueItem videoItem = TestPlayQueueItem.createVideo(AdFixtures.getVideoAd(TRACK_URN, videoSource));
        when(adsOperations.isCurrentItemAd()).thenReturn(true);
        when(adsOperations.isCurrentItemVideoAd()).thenReturn(true);
        when(adsOperations.getCurrentTrackAdData()).thenReturn(videoItem.getAdData());
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(videoItem, Urn.NOT_SET, 0));
    }

    private void resumeFromBackground() {
        controller.onResume(activity);
        controller.onPause(activity);
        controller.onResume(activity);
    }
}
