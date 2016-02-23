package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.mediaplayer.MediaPlayerAudioAdapter;
import com.soundcloud.android.playback.mediaplayer.MediaPlayerVideoAdapter;
import com.soundcloud.android.playback.skippy.SkippyAdapter;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class StreamPlayerTest extends AndroidUnitTest {

    private StreamPlayer streamPlayerWrapper;
    @Mock private MediaPlayerAudioAdapter mediaPlayerAudioAdapter;
    @Mock private MediaPlayerVideoAdapter mediaPlayerVideoAdapter;
    @Mock private SkippyAdapter skippyAdapter;
    @Mock private Player.PlayerListener playerListener;
    @Mock private NetworkConnectionHelper networkConnectionHelper;

    private TestEventBus eventBus = new TestEventBus();

    private Urn trackUrn = Urn.forTrack(123L);
    private PropertySet track = PropertySet.from(
            TrackProperty.URN.bind(trackUrn),
            TrackProperty.PLAY_DURATION.bind(456L)
    );
    private AudioPlaybackItem audioPlaybackItem = AudioPlaybackItem.create(track, 123L);
    private AudioPlaybackItem offlinePlaybackItem = AudioPlaybackItem.forOffline(track, 123L);
    private VideoPlaybackItem videoPlaybackItem = VideoPlaybackItem.create(AdFixtures.getVideoAd(trackUrn));

    @Before
    public void setUp() throws Exception {
        when(skippyAdapter.init()).thenReturn(true);
    }

    @After
    public void tearDown() {
        StreamPlayer.skippyFailedToInitialize = false;
    }

    private void instantiateStreamPlaya() {
        streamPlayerWrapper = new StreamPlayer(mediaPlayerAudioAdapter, mediaPlayerVideoAdapter, skippyAdapter, networkConnectionHelper, eventBus);
        streamPlayerWrapper.setListener(playerListener);
    }

    @Test
    public void initCallsInitOnSkippy() {
        instantiateStreamPlaya();
        verify(skippyAdapter).init();
    }

    @Test
    public void initDoesNotCallInitOnSkippyIfInitAlreadyFailed() {
        when(skippyAdapter.init()).thenReturn(false);
        instantiateStreamPlaya();
        reset(skippyAdapter);
        instantiateStreamPlaya();
        verifyZeroInteractions(skippyAdapter);
    }

    @Test
    public void preloadCallsPreloadOnSkippy() {
        final AutoParcel_PreloadItem preloadItem = new AutoParcel_PreloadItem(trackUrn, PlaybackType.AUDIO_SNIPPET);
        instantiateStreamPlaya();

        streamPlayerWrapper.preload(preloadItem);

        verify(skippyAdapter).preload(preloadItem);
    }

    @Test
    public void playCallsPlayOnSkippyByDefault() {
        instantiateStreamPlaya();

        startPlaybackOnSkippy();

        verify(skippyAdapter).play(audioPlaybackItem);
    }

    @Test
    public void playSetsListenerOnSkippyByDefault() {
        instantiateStreamPlaya();

        startPlaybackOnSkippy();

        verify(skippyAdapter).setListener(streamPlayerWrapper);
    }

    @Test
    public void playWithUninterruptedPlaybackItemCallsPlayOnSkippyByDefault() {
        final AudioPlaybackItem uninterruptedItem = AudioPlaybackItem.forAudioAd(track);
        instantiateStreamPlaya();

        streamPlayerWrapper.play(uninterruptedItem);

        verify(skippyAdapter).play(uninterruptedItem);
    }

    @Test
    public void playPlaysOnMediaPlayerIfSkippyLoadFailed() {
        when(skippyAdapter.init()).thenReturn(false);
        instantiateStreamPlaya();

        startPlaybackOnSkippy();

        verify(mediaPlayerAudioAdapter).play(audioPlaybackItem);
    }

    @Test
    public void playOfflineCallsPlayOfflineOnSkippyWithResumeTime() {
        final AudioPlaybackItem offlineItem = AudioPlaybackItem.forOffline(track, 123L);
        instantiateStreamPlaya();

        streamPlayerWrapper.play(offlineItem);

        verify(skippyAdapter).play(offlineItem);
    }

    @Test
    public void playCallsPlayOnMediaPlayerAndSkippyFailedToInitialize() {
        when(skippyAdapter.init()).thenReturn(false);
        instantiateStreamPlaya();

        startPlaybackOnSkippy();

        verify(mediaPlayerAudioAdapter).play(audioPlaybackItem);
    }

    @Test
    public void playLogsErrorOnOfflinePlayWhenSkippyFailedToInitialize() {
        when(skippyAdapter.init()).thenReturn(false);
        instantiateStreamPlaya();

        streamPlayerWrapper.play(offlinePlaybackItem);

        assertThat(eventBus.lastEventOn(EventQueue.PLAYBACK_ERROR).getCategory())
                .isEqualTo(PlaybackErrorEvent.CATEGORY_OFFLINE_PLAY_UNAVAILABLE);
    }

    @Test
    public void playSetsListenerToSkippy() {
        instantiateStreamPlaya();

        startPlaybackOnSkippy();
        
        verify(skippyAdapter).setListener(streamPlayerWrapper);
    }

    @Test
    public void playVideoItemCallsVideoPlayerAdapterPlayVideo() {
        instantiateStreamPlaya();

        startPlaybackOnVideoPlayerAdapter(videoPlaybackItem);

        verify(mediaPlayerVideoAdapter).play(videoPlaybackItem);
    }

    @Test
    public void resumeCallsResumeOnVideoPlayer() {
        instantiateStreamPlaya();
        startPlaybackOnVideoPlayerAdapter(videoPlaybackItem);

        streamPlayerWrapper.resume();

        verify(mediaPlayerVideoAdapter).resume();
    }

    @Test
    public void pauseCallsPauseOnVideoPlayer() {
        instantiateStreamPlaya();
        startPlaybackOnVideoPlayerAdapter(videoPlaybackItem);

        streamPlayerWrapper.pause();

        verify(mediaPlayerVideoAdapter).pause();
    }

    @Test
    public void isSeekableReturnsFalseOnVideoPlayer() {
        instantiateStreamPlaya();
        startPlaybackOnVideoPlayerAdapter(videoPlaybackItem);

        assertThat(streamPlayerWrapper.isSeekable()).isFalse();
    }

    @Test
    public void getProgressCallsGetProgressOnVideoPlayer() {
        instantiateStreamPlaya();
        startPlaybackOnVideoPlayerAdapter(videoPlaybackItem);

        streamPlayerWrapper.getProgress();
        verify(mediaPlayerVideoAdapter).getProgress();
    }

    @Test
    public void setVolumeCallsSetVolumeOnVideoPlayer() {
        instantiateStreamPlaya();
        startPlaybackOnVideoPlayerAdapter(videoPlaybackItem);

        streamPlayerWrapper.setVolume(3.0f);
        verify(mediaPlayerVideoAdapter).setVolume(3.0f);
    }

    @Test
    public void stopCallsStopOnVideoPlayer() {
        instantiateStreamPlaya();
        startPlaybackOnVideoPlayerAdapter(videoPlaybackItem);

        streamPlayerWrapper.stop();
        verify(mediaPlayerVideoAdapter).stop();
    }

    @Test
    public void resumeCallsResumeOnMediaPlayer() {
        instantiateStreamPlaya();
        fallBackToMediaPlayer();
        streamPlayerWrapper.resume();
        verify(mediaPlayerAudioAdapter).resume();
    }

    @Test
    public void pauseCallsPauseOnMediaPlayer() {
        instantiateStreamPlaya();
        fallBackToMediaPlayer();
        streamPlayerWrapper.pause();
        verify(mediaPlayerAudioAdapter).pause();
    }

    @Test
    public void seekCallsSeekOnMediaPlayer() {
        instantiateStreamPlaya();
        fallBackToMediaPlayer();
        streamPlayerWrapper.seek(100, true);
        verify(mediaPlayerAudioAdapter).seek(100, true);
    }

    @Test
    public void getProgressCallsGetProgressOnMediaPlayer() {
        instantiateStreamPlaya();
        fallBackToMediaPlayer();
        streamPlayerWrapper.getProgress();
        verify(mediaPlayerAudioAdapter).getProgress();
    }

    @Test
    public void setVolumeCallsSetVolumeOnMediaPlayer() {
        instantiateStreamPlaya();
        fallBackToMediaPlayer();
        streamPlayerWrapper.setVolume(3.0f);
        verify(mediaPlayerAudioAdapter).setVolume(3.0f);
    }

    @Test
    public void stopCallsStopOnMediaPlayer() {
        instantiateStreamPlaya();
        fallBackToMediaPlayer();
        streamPlayerWrapper.stop();
        verify(mediaPlayerAudioAdapter).stop();
    }

    @Test
    public void isSeekableReturnsMediaPlayerIsSeekable() {
        instantiateStreamPlaya();
        fallBackToMediaPlayer();
        when(mediaPlayerAudioAdapter.isSeekable()).thenReturn(true);
        assertThat(streamPlayerWrapper.isSeekable()).isTrue();
    }

    @Test
    public void resumeCallsResumeOnSkippyPlayer() {
        instantiateStreamPlaya();
        startPlaybackOnSkippy();
        streamPlayerWrapper.resume();
        verify(skippyAdapter).resume();
    }

    @Test
    public void pauseCallsPauseOnSkippy() {
        instantiateStreamPlaya();
        startPlaybackOnSkippy();
        streamPlayerWrapper.pause();
        verify(skippyAdapter).pause();
    }

    @Test
    public void seekCallsSeekOnSkippy() {
        instantiateStreamPlaya();
        startPlaybackOnSkippy();
        streamPlayerWrapper.seek(100, true);
        verify(skippyAdapter).seek(100, true);
    }

    @Test
    public void getProgressCallsGetProgressOnSkippy() {
        instantiateStreamPlaya();
        startPlaybackOnSkippy();
        streamPlayerWrapper.getProgress();
        verify(skippyAdapter).getProgress();
    }

    @Test
    public void setVolumeCallsSetVolumeOnSkippy() {
        instantiateStreamPlaya();
        startPlaybackOnSkippy();
        streamPlayerWrapper.setVolume(3.0f);
        verify(skippyAdapter).setVolume(3.0f);
    }

    @Test
    public void stopCallsStopOnSkippy() {
        instantiateStreamPlaya();
        startPlaybackOnSkippy();
        streamPlayerWrapper.stop();
        verify(skippyAdapter).stop();
    }

    @Test
    public void isSeekableReturnsSkippyIsSeekable() {
        instantiateStreamPlaya();
        startPlaybackOnSkippy();
        when(skippyAdapter.isSeekable()).thenReturn(true);
        assertThat(streamPlayerWrapper.isSeekable()).isTrue();
    }

    @Test
    public void destroyCallsDestroyOnAllPlayers() {
        instantiateStreamPlaya();
        streamPlayerWrapper.destroy();
        verify(mediaPlayerVideoAdapter).destroy();
        verify(mediaPlayerAudioAdapter).destroy();
        verify(skippyAdapter).destroy();
    }

    @Test(expected = NullPointerException.class)
    public void onPlaystateChangeDoesThrowsNPEWithSuccessStateAndNoListener() {
        instantiateStreamPlaya();
        streamPlayerWrapper.setListener(null);
        streamPlayerWrapper.onPlaystateChanged(TestPlayStates.buffering());
    }

    @Test
    public void onPlayStateChangedPassesSuccessEventToListener() {
        instantiateStreamPlaya();
        streamPlayerWrapper.setListener(playerListener);

        final Player.StateTransition transition = TestPlayStates.buffering();
        streamPlayerWrapper.onPlaystateChanged(transition);
        verify(playerListener).onPlaystateChanged(transition);
    }

    @Test
    public void autoRetriesLastPlayOnMediaPlayerIfSkippyErrorsWhileConnectedToInternet() {
        instantiateStreamPlaya();
        when(skippyAdapter.getProgress()).thenReturn(123L);
        
        startPlaybackOnSkippy();
        
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(true);

        streamPlayerWrapper.onPlaystateChanged(new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.ERROR_FAILED, trackUrn));
        verify(mediaPlayerAudioAdapter).play(audioPlaybackItem);
    }

    @Test
    public void doesNotAutoRetryLastPlayOnMediaPlayerIfSkippyErrorsWithForbidden() {
        instantiateStreamPlaya();
        when(skippyAdapter.getProgress()).thenReturn(123L);
        
        startPlaybackOnSkippy();
        
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(true);

        streamPlayerWrapper.onPlaystateChanged(new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.ERROR_FORBIDDEN, trackUrn));
        verify(mediaPlayerAudioAdapter, never()).play(any(PlaybackItem.class));
    }

    @Test
    public void doesNotAutoRetryLastPlayOnMediaPlayerIfSkippyErrorsWithNotFound() {
        instantiateStreamPlaya();
        when(skippyAdapter.getProgress()).thenReturn(123L);

        startPlaybackOnSkippy();
        
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(true);

        streamPlayerWrapper.onPlaystateChanged(new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.ERROR_NOT_FOUND, trackUrn));
        verify(mediaPlayerAudioAdapter, never()).play(any(PlaybackItem.class));
    }

    @Test
    public void onPlaystateChangedPassesErrorEventToListenerWhenNotConnectedToInternet() {
        instantiateStreamPlaya();
        startPlaybackOnSkippy();
        when(skippyAdapter.getProgress()).thenReturn(1L);

        final Player.StateTransition stateTransition = new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.ERROR_NOT_FOUND, Urn.forTrack(123L));
        streamPlayerWrapper.onPlaystateChanged(stateTransition);
        verify(playerListener).onPlaystateChanged(stateTransition);
    }

    @Test
    public void requestsFocusOnListener() {
        instantiateStreamPlaya();
        streamPlayerWrapper.requestAudioFocus();
        verify(playerListener).requestAudioFocus();
    }

    @Test(expected = NullPointerException.class)
    public void requestsFocusThrowsNPEWithNoListener() {
        instantiateStreamPlaya();
        streamPlayerWrapper.setListener(null);
        streamPlayerWrapper.requestAudioFocus();
    }

    @Test
    public void getStateReturnsIdleByDefault() {
        instantiateStreamPlaya();
        assertThat(streamPlayerWrapper.getState()).isSameAs(Player.PlayerState.IDLE);
    }

    @Test
    public void getStateReturnsLastState() {
        instantiateStreamPlaya();
        streamPlayerWrapper.onPlaystateChanged(TestPlayStates.buffering());
        assertThat(streamPlayerWrapper.getState()).isSameAs(Player.PlayerState.BUFFERING);
    }

    @Test
    public void getLastStateTransitionReturnsIdleNothingByDefault() {
        instantiateStreamPlaya();
        assertThat(streamPlayerWrapper.getLastStateTransition()).isEqualTo(Player.StateTransition.DEFAULT);
    }

    @Test
    public void getLastStateTransitionReturnsLastTransition() {
        instantiateStreamPlaya();
        final Player.StateTransition stateTransition = TestPlayStates.buffering();
        streamPlayerWrapper.onPlaystateChanged(stateTransition);
        assertThat(streamPlayerWrapper.getLastStateTransition()).isEqualTo(stateTransition);
    }

    @Test
    public void isPlayingReturnsIsPlayingFromLastTransition() {
        instantiateStreamPlaya();
        streamPlayerWrapper.onPlaystateChanged(TestPlayStates.buffering());
        assertThat(streamPlayerWrapper.isPlaying()).isTrue();
    }

    @Test
    public void isPlayerPlayingReturnsIsPlayerPlayingFromLastTransition() {
        instantiateStreamPlaya();
        streamPlayerWrapper.onPlaystateChanged(TestPlayStates.playing());
        assertThat(streamPlayerWrapper.isPlaying()).isTrue();
    }

    @Test
    public void isBufferingReturnsIsPlayerPlayingFromLastTransition() {
        instantiateStreamPlaya();
        streamPlayerWrapper.onPlaystateChanged(TestPlayStates.buffering());
        assertThat(streamPlayerWrapper.isPlaying()).isTrue();
    }

    @Test
    public void shouldNotDestroySkippyIfInitialisationFailed() {
        when(skippyAdapter.init()).thenReturn(false);
        instantiateStreamPlaya();
        streamPlayerWrapper.destroy();
        verify(skippyAdapter, never()).destroy();
    }

    private void fallBackToMediaPlayer() {
        startPlaybackOnSkippy();
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(true);
        streamPlayerWrapper.onPlaystateChanged(new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.ERROR_FAILED, trackUrn));
    }

    private void startPlaybackOnSkippy() {
        streamPlayerWrapper.play(audioPlaybackItem);
    }

    private void startPlaybackOnVideoPlayerAdapter(VideoPlaybackItem videoPlaybackItem) {
        streamPlayerWrapper.play(videoPlaybackItem);
    }
}
