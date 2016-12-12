package com.soundcloud.android.ads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.VideoAdQueueItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.support.v7.app.AppCompatActivity;

public class AdOrientationControllerTest extends AndroidUnitTest {

    @Mock private AdsOperations adsOperations;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private DeviceHelper deviceHelper;
    @Mock private AppCompatActivity activity;

    private TestEventBus eventBus = new TestEventBus();
    private AdOrientationController controller;

    @Before
    public void setUp() throws Exception {
        controller = new AdOrientationController(adsOperations, eventBus, deviceHelper, playQueueManager);
    }

    @Test
    public void forcePlayerLandscapeShouldChangeOrientationForLetterboxVideoAd() {
        setVideoAdIsPlaying(false);

        controller.onResume(activity);
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.forcePlayerLandscape());

        verify(activity).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Test
    public void forcePlayerPortraitShouldChangeOrientationForLetterboxVideoAd() {
        setVideoAdIsPlaying(false);

        controller.onResume(activity);
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.forcePlayerPortrait());

        verify(activity).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Test
    public void forcePlayerLandscapeShouldNotChangeOrientationForTracks() {
        setAudioPlaying(false);

        controller.onResume(activity);
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.forcePlayerLandscape());

        verify(activity, never()).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Test
    public void forcePlayerLandscapeShouldNotChangeOrientationForAudioAds() {
        setAudioPlaying(true);

        controller.onResume(activity);
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.forcePlayerLandscape());

        verify(activity, never()).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Test
    public void forcePlayerPortraitShouldNotChangeOrientationForTracks() {
        setAudioPlaying(false);

        controller.onResume(activity);
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.forcePlayerPortrait());

        verify(activity, never()).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Test
    public void forcePlayerPortraitShouldNotChangeOrientationForAudioAds() {
        setAudioPlaying(true);

        controller.onResume(activity);
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.forcePlayerPortrait());

        verify(activity, never()).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Test
    public void unlocksPlayerAndResetOrientationToUnspecifiedWhenRegularTrackIsPlaying() {
        setAudioPlaying(false);

        controller.onResume(activity);

        verify(activity).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    @Test
    public void locksOrientationToPortraitWhenVerticalVideoIsPlayed() {
        setVideoAdIsPlaying(true);

        controller.onResume(activity);

        verify(activity).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Test
    public void neverLockOrientationToPortraitWhenLetterboxVideoIsPlayed() {
        setVideoAdIsPlaying(false);

        controller.onResume(activity);

        verify(activity, never()).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Test
    public void orientationChangeToLandscapeSendsFullscreenVideoAdUIEvent() {
        when(activity.isChangingConfigurations()).thenReturn(true);
        when(deviceHelper.isOrientation(Configuration.ORIENTATION_LANDSCAPE)).thenReturn(true);
        setVideoAdIsPlaying(true);

        controller.onPause(activity);
        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).getKind()).isEqualTo(UIEvent.Kind.VIDEO_AD_FULLSCREEN.toString());
    }

    @Test
    public void orientationChangeToPortraitSendsShrinkVideoAdUIEvent() {
        when(activity.isChangingConfigurations()).thenReturn(true);
        when(deviceHelper.isOrientation(Configuration.ORIENTATION_PORTRAIT)).thenReturn(true);
        setVideoAdIsPlaying(true);

        controller.onPause(activity);
        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).getKind()).isEqualTo(UIEvent.Kind.VIDEO_AD_SHRINK.toString());
    }

    private void setAudioPlaying(boolean isAd) {
        when(adsOperations.isCurrentItemAd()).thenReturn(isAd);
        when(adsOperations.isCurrentItemAudioAd()).thenReturn(isAd);
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(Urn.forTrack(123L)),
                                                                       Urn.NOT_SET,
                                                                       0));
    }

    private void setVideoAdIsPlaying(boolean verticalVideo) {
        final ApiVideoSource videoSource = verticalVideo ?
                                           AdFixtures.getApiVideoSource(300, 600) :
                                           AdFixtures.getApiVideoSource(600, 300);
        final VideoAdQueueItem videoItem = TestPlayQueueItem.createVideo(AdFixtures.getVideoAd(Urn.forTrack(123L),
                                                                                             videoSource));
        when(adsOperations.isCurrentItemAd()).thenReturn(true);
        when(adsOperations.isCurrentItemVideoAd()).thenReturn(true);
        when(adsOperations.getCurrentTrackAdData()).thenReturn(videoItem.getAdData());
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(videoItem, Urn.NOT_SET, 0));
    }

}
