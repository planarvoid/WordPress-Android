package com.soundcloud.android.playback;

import static com.soundcloud.android.ads.AdFixtures.getVideoAd;
import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.AdFixtures;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.flipper.FlipperAdapter;
import com.soundcloud.android.playback.mediaplayer.MediaPlayerAdapter;
import com.soundcloud.android.playback.skippy.SkippyAdapter;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayerTransitions;
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
    @Mock private MediaPlayerAdapter mediaPlayerAdapter;
    @Mock private SkippyAdapter skippyAdapter;
    @Mock private FlipperAdapter flipperAdapter;
    @Mock private Player.PlayerListener playerListener;
    @Mock private NetworkConnectionHelper networkConnectionHelper;
    @Mock private FeatureFlags featureFlags;

    private TestEventBus eventBus = new TestEventBus();

    private Urn trackUrn = Urn.forTrack(123L);
    private PropertySet track = PropertySet.from(
            TrackProperty.URN.bind(trackUrn),
            TrackProperty.SNIPPED.bind(false),
            TrackProperty.SNIPPET_DURATION.bind(345L),
            TrackProperty.FULL_DURATION.bind(456L)
    );
    private AudioPlaybackItem audioPlaybackItem = AudioPlaybackItem.create(track, 123L);
    private AudioAdPlaybackItem audioAdPlaybackItem = AudioAdPlaybackItem.create(AdFixtures.getAudioAd(trackUrn));
    private AudioPlaybackItem offlinePlaybackItem = AudioPlaybackItem.forOffline(track, 123L);
    private VideoAdPlaybackItem videoPlaybackItem = VideoAdPlaybackItem.create(getVideoAd(trackUrn), 0L);

    @Before
    public void setUp() throws Exception {
        when(skippyAdapter.init()).thenReturn(true);
        when(featureFlags.isEnabled(Flag.FLIPPER)).thenReturn(false);
    }

    @After
    public void tearDown() {
        StreamPlayer.SKIPPY_FAILED_TO_INITIALIZE = false;
    }

    private void instantiateStreamPlaya() {
        streamPlayerWrapper = new StreamPlayer(mediaPlayerAdapter, skippyAdapter, providerOf(flipperAdapter), networkConnectionHelper, eventBus, featureFlags);
        streamPlayerWrapper.setListener(playerListener);
    }

    @Test
    public void initCallsInitOnSkippy() {
        instantiateStreamPlaya();
        verify(skippyAdapter).init();
    }

    @Test
    // TODO: Do we need it ? For Flipper also ?
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
    public void playWithAudioAdPlaybackItemCallsPlayOnSkippyByDefault() {
        final AudioAd audioAd = AdFixtures.getAudioAd(trackUrn);
        final AudioAdPlaybackItem audioAdPlaybackItem = AudioAdPlaybackItem.create(audioAd);
        instantiateStreamPlaya();

        streamPlayerWrapper.play(audioAdPlaybackItem);

        verify(skippyAdapter).play(audioAdPlaybackItem);
    }

    @Test
    public void playPlaysOnMediaPlayerIfSkippyLoadFailed() {
        when(skippyAdapter.init()).thenReturn(false);
        instantiateStreamPlaya();

        startPlaybackOnSkippy();

        verify(mediaPlayerAdapter).play(audioPlaybackItem);
    }

    @Test
    public void playOfflineCallsPlayOfflineOnSkippyWithResumeTime() {
        final AudioPlaybackItem offlineItem = AudioPlaybackItem.forOffline(track, 123L);
        instantiateStreamPlaya();

        startPlaybackOnSkippy(offlineItem);

        verify(skippyAdapter).play(offlineItem);
    }

    @Test
    public void playCallsPlayOnMediaPlayerAndSkippyFailedToInitialize() {
        when(skippyAdapter.init()).thenReturn(false);
        instantiateStreamPlaya();

        startPlaybackOnSkippy();

        verify(mediaPlayerAdapter).play(audioPlaybackItem);
    }

    @Test
    public void playLogsErrorOnOfflinePlayWhenSkippyFailedToInitialize() {
        when(skippyAdapter.init()).thenReturn(false);
        instantiateStreamPlaya();

        startPlaybackOnSkippy(offlinePlaybackItem);

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
    public void playVideoItemCallsMediaPlayerAdapterPlayVideo() {
        instantiateStreamPlaya();

        startPlaybackOnVideoPlayerAdapter(videoPlaybackItem);

        verify(mediaPlayerAdapter).play(videoPlaybackItem);
    }

    @Test
    public void resumeCallsResumeOnVideoPlayer() {
        instantiateStreamPlaya();
        startPlaybackOnVideoPlayerAdapter(videoPlaybackItem);

        streamPlayerWrapper.resume(videoPlaybackItem);

        verify(mediaPlayerAdapter).resume(videoPlaybackItem);
    }

    @Test
    public void pauseCallsPauseOnVideoPlayer() {
        instantiateStreamPlaya();
        startPlaybackOnVideoPlayerAdapter(videoPlaybackItem);

        streamPlayerWrapper.pause();

        verify(mediaPlayerAdapter).pause();
    }

    @Test
    public void isSeekableCallsIsSeekableOnVideoPlayer() {
        instantiateStreamPlaya();
        startPlaybackOnVideoPlayerAdapter(videoPlaybackItem);

        streamPlayerWrapper.isSeekable();

        verify(mediaPlayerAdapter).isSeekable();
    }

    @Test
    public void getProgressCallsGetProgressOnVideoPlayer() {
        instantiateStreamPlaya();
        startPlaybackOnVideoPlayerAdapter(videoPlaybackItem);

        streamPlayerWrapper.getProgress();
        verify(mediaPlayerAdapter).getProgress();
    }

    @Test
    public void setVolumeCallsSetVolumeOnVideoPlayer() {
        instantiateStreamPlaya();
        startPlaybackOnVideoPlayerAdapter(videoPlaybackItem);

        streamPlayerWrapper.setVolume(3.0f);
        verify(mediaPlayerAdapter).setVolume(3.0f);
    }

    @Test
    public void getVolumeCallsGetVolumeOnVideoPlayer() {
        instantiateStreamPlaya();
        startPlaybackOnVideoPlayerAdapter(videoPlaybackItem);

        streamPlayerWrapper.getVolume();
        verify(mediaPlayerAdapter).getVolume();
    }

    @Test
    public void stopCallsStopOnVideoPlayer() {
        instantiateStreamPlaya();
        startPlaybackOnVideoPlayerAdapter(videoPlaybackItem);

        streamPlayerWrapper.stop();
        verify(mediaPlayerAdapter).stop();
    }

    @Test
    public void resumeCallsResumeOnMediaPlayer() {
        instantiateStreamPlaya();
        startsAndFailsPlaybackOnSkippy();
        streamPlayerWrapper.resume(videoPlaybackItem);
        verify(mediaPlayerAdapter).resume(videoPlaybackItem);
    }

    @Test
    public void pauseCallsPauseOnMediaPlayer() {
        instantiateStreamPlaya();
        startsAndFailsPlaybackOnSkippy();
        streamPlayerWrapper.pause();
        verify(mediaPlayerAdapter).pause();
    }

    @Test
    public void seekCallsSeekOnMediaPlayer() {
        instantiateStreamPlaya();
        startsAndFailsPlaybackOnSkippy();
        streamPlayerWrapper.seek(100, true);
        verify(mediaPlayerAdapter).seek(100, true);
    }

    @Test
    public void getProgressCallsGetProgressOnMediaPlayer() {
        instantiateStreamPlaya();
        startsAndFailsPlaybackOnSkippy();
        streamPlayerWrapper.getProgress();
        verify(mediaPlayerAdapter).getProgress();
    }

    @Test
    public void setVolumeCallsSetVolumeOnMediaPlayer() {
        instantiateStreamPlaya();
        startsAndFailsPlaybackOnSkippy();
        streamPlayerWrapper.setVolume(3.0f);
        verify(mediaPlayerAdapter).setVolume(3.0f);
    }

    @Test
    public void getVolumeCallsGetVolumeOnMediaPlayer() {
        instantiateStreamPlaya();
        startsAndFailsPlaybackOnSkippy();
        streamPlayerWrapper.getVolume();
        verify(mediaPlayerAdapter).getVolume();
    }

    @Test
    public void stopCallsStopOnMediaPlayer() {
        instantiateStreamPlaya();
        startsAndFailsPlaybackOnSkippy();
        streamPlayerWrapper.stop();
        verify(mediaPlayerAdapter).stop();
    }

    @Test
    public void isSeekableReturnsMediaPlayerIsSeekable() {
        instantiateStreamPlaya();
        startsAndFailsPlaybackOnSkippy();
        when(mediaPlayerAdapter.isSeekable()).thenReturn(true);
        assertThat(streamPlayerWrapper.isSeekable()).isTrue();
    }

    @Test
    public void resumeCallsResumeOnSkippyPlayer() {
        instantiateStreamPlaya();
        startPlaybackOnSkippy();
        streamPlayerWrapper.resume(audioPlaybackItem);
        verify(skippyAdapter).resume(audioPlaybackItem);
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
    public void getVolumeCallsGetVolumeOnSkippy() {
        instantiateStreamPlaya();
        startPlaybackOnSkippy();
        streamPlayerWrapper.getVolume();
        verify(skippyAdapter).getVolume();
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
        verify(mediaPlayerAdapter).destroy();
        verify(skippyAdapter).destroy();
    }

    @Test(expected = NullPointerException.class)
    public void onPlaystateChangeDoesThrowsNPEWithSuccessStateAndNoListener() {
        instantiateStreamPlaya();
        streamPlayerWrapper.setListener(null);
        streamPlayerWrapper.onPlaystateChanged(TestPlayerTransitions.buffering());
    }

    @Test
    public void onPlayStateChangedPassesSuccessEventToListener() {
        instantiateStreamPlaya();
        streamPlayerWrapper.setListener(playerListener);

        final PlaybackStateTransition transition = TestPlayerTransitions.buffering();
        streamPlayerWrapper.onPlaystateChanged(transition);
        verify(playerListener).onPlaystateChanged(transition);
    }

    @Test
    public void autoRetriesLastPlayOnMediaPlayerIfSkippyErrorsWhileConnectedToInternet() {
        instantiateStreamPlaya();
        when(skippyAdapter.getProgress()).thenReturn(123L);

        startPlaybackOnSkippy();

        when(networkConnectionHelper.isNetworkConnected()).thenReturn(true);

        streamPlayerWrapper.onPlaystateChanged(idleTransition(PlayStateReason.ERROR_FAILED, trackUrn));
        verify(mediaPlayerAdapter).play(audioPlaybackItem);
    }

    @Test
    public void doesNotAutoRetryLastPlayOnMediaPlayerIfSkippyErrorsWithForbidden() {
        instantiateStreamPlaya();
        when(skippyAdapter.getProgress()).thenReturn(123L);

        startPlaybackOnSkippy();

        when(networkConnectionHelper.isNetworkConnected()).thenReturn(true);

        streamPlayerWrapper.onPlaystateChanged(idleTransition(PlayStateReason.ERROR_FORBIDDEN, trackUrn));
        verify(mediaPlayerAdapter, never()).play(any(PlaybackItem.class));
    }

    @Test
    public void doesNotAutoRetryLastPlayOnMediaPlayerIfSkippyErrorsWithNotFound() {
        instantiateStreamPlaya();
        when(skippyAdapter.getProgress()).thenReturn(123L);

        startPlaybackOnSkippy();

        when(networkConnectionHelper.isNetworkConnected()).thenReturn(true);

        streamPlayerWrapper.onPlaystateChanged(idleTransition(PlayStateReason.ERROR_NOT_FOUND, trackUrn));
        verify(mediaPlayerAdapter, never()).play(any(PlaybackItem.class));
    }

    @Test
    public void onPlaystateChangedPassesErrorEventToListenerWhenNotConnectedToInternet() {
        instantiateStreamPlaya();
        startPlaybackOnSkippy();
        when(skippyAdapter.getProgress()).thenReturn(1L);

        final PlaybackStateTransition stateTransition = idleTransition(PlayStateReason.ERROR_NOT_FOUND,
                                                                       Urn.forTrack(123L));
        streamPlayerWrapper.onPlaystateChanged(stateTransition);
        verify(playerListener).onPlaystateChanged(stateTransition);
    }

    @Test
    public void getStateReturnsIdleByDefault() {
        instantiateStreamPlaya();
        assertThat(streamPlayerWrapper.getState()).isSameAs(PlaybackState.IDLE);
    }

    @Test
    public void getStateReturnsLastState() {
        instantiateStreamPlaya();
        streamPlayerWrapper.onPlaystateChanged(TestPlayerTransitions.buffering());
        assertThat(streamPlayerWrapper.getState()).isSameAs(PlaybackState.BUFFERING);
    }

    @Test
    public void getLastStateTransitionReturnsIdleNothingByDefault() {
        instantiateStreamPlaya();
        assertThat(streamPlayerWrapper.getLastStateTransition()).isEqualTo(PlaybackStateTransition.DEFAULT);
    }

    @Test
    public void getLastStateTransitionReturnsLastTransition() {
        instantiateStreamPlaya();
        final PlaybackStateTransition stateTransition = TestPlayerTransitions.buffering();
        streamPlayerWrapper.onPlaystateChanged(stateTransition);
        assertThat(streamPlayerWrapper.getLastStateTransition()).isEqualTo(stateTransition);
    }

    @Test
    public void isPlayingReturnsIsPlayingFromLastTransition() {
        instantiateStreamPlaya();
        streamPlayerWrapper.onPlaystateChanged(TestPlayerTransitions.buffering());
        assertThat(streamPlayerWrapper.isPlaying()).isTrue();
    }

    @Test
    public void isPlayerPlayingReturnsIsPlayerPlayingFromLastTransition() {
        instantiateStreamPlaya();
        streamPlayerWrapper.onPlaystateChanged(TestPlayerTransitions.playing());
        assertThat(streamPlayerWrapper.isPlaying()).isTrue();
    }

    @Test
    public void isBufferingReturnsIsPlayerPlayingFromLastTransition() {
        instantiateStreamPlaya();
        streamPlayerWrapper.onPlaystateChanged(TestPlayerTransitions.buffering());
        assertThat(streamPlayerWrapper.isPlaying()).isTrue();
    }

    @Test
    public void shouldNotDestroySkippyIfInitialisationFailed() {
        when(skippyAdapter.init()).thenReturn(false);
        instantiateStreamPlaya();
        streamPlayerWrapper.destroy();
        verify(skippyAdapter, never()).destroy();
    }

    @Test
    public void shouldFallbackToMediaPlayerOnSkippyFailureForAudioAds() {
        instantiateStreamPlaya();

        streamPlayerWrapper.play(audioAdPlaybackItem);
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(true);
        streamPlayerWrapper.onPlaystateChanged(idleTransition(PlayStateReason.ERROR_FAILED, trackUrn));

        verify(mediaPlayerAdapter).play(audioAdPlaybackItem);
    }

    @Test
    public void shouldNotFallbackToMediaPlayerOnSkippyFailureForOfflineTracks() {
        instantiateStreamPlaya();

        final PlaybackStateTransition errorTransition = startsAndFailsPlaybackOnSkippy(offlinePlaybackItem);

        verify(playerListener).onPlaystateChanged(errorTransition);
        verifyNoMoreInteractions(mediaPlayerAdapter);
    }

    private PlaybackStateTransition startsAndFailsPlaybackOnSkippy() {
        final PlaybackStateTransition stateTransition = idleTransition(PlayStateReason.ERROR_FAILED, trackUrn);

        startPlaybackOnSkippy();
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(true);
        streamPlayerWrapper.onPlaystateChanged(stateTransition);

        return stateTransition;
    }

    private PlaybackStateTransition startsAndFailsPlaybackOnSkippy(AudioPlaybackItem audioPlaybackItem) {
        final PlaybackStateTransition stateTransition = idleTransition(PlayStateReason.ERROR_FAILED, audioPlaybackItem.getUrn());

        startPlaybackOnSkippy(audioPlaybackItem);
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(true);
        streamPlayerWrapper.onPlaystateChanged(stateTransition);

        return stateTransition;
    }

    private PlaybackStateTransition idleTransition(PlayStateReason errorFailed, Urn urn) {
        return new PlaybackStateTransition(PlaybackState.IDLE, errorFailed, urn, 0, 0);
    }

    private void startPlaybackOnSkippy() {
        startPlaybackOnSkippy(audioPlaybackItem);
    }

    private void startPlaybackOnSkippy(AudioPlaybackItem audioPlaybackItem) {
        streamPlayerWrapper.play(audioPlaybackItem);
    }

    private void startPlaybackOnVideoPlayerAdapter(VideoAdPlaybackItem videoPlaybackItem) {
        streamPlayerWrapper.play(videoPlaybackItem);
    }
}
