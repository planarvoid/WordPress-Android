package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.playback.mediaplayer.MediaPlayerAdapter;
import com.soundcloud.android.playback.skippy.SkippyAdapter;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.collections.PropertySet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Context;

public class StreamPlayerTest extends AndroidUnitTest {

    private final Urn trackUrn = Urn.forTrack(1L);

    private StreamPlayer streamPlayerWrapper;
    @Mock private Context context;
    @Mock private MediaPlayerAdapter mediaPlayerAdapter;
    @Mock private SkippyAdapter skippyAdapter;
    @Mock private BufferingPlayer bufferingPlaya;
    @Mock private Player.PlayerListener playerListener;
    @Mock private OfflinePlaybackOperations offlinePlaybackOps;
    @Mock private NetworkConnectionHelper networkConnectionHelper;

    private PropertySet track;

    @Before
    public void setUp() throws Exception {
        track = TestPropertySets.expectedTrackForPlayer();
        when(skippyAdapter.init(context)).thenReturn(true);
    }

    @After
    public void tearDown() {
        StreamPlayer.skippyFailedToInitialize = false;
    }

    private void instantiateStreamPlaya() {
        streamPlayerWrapper = new StreamPlayer(context, mediaPlayerAdapter, skippyAdapter, bufferingPlaya, offlinePlaybackOps, networkConnectionHelper);
        streamPlayerWrapper.setListener(playerListener);
    }

    @Test
    public void initCallsInitOnSkippy() {
        instantiateStreamPlaya();
        verify(skippyAdapter).init(context);
    }

    @Test
    public void initDoesNotCallInitOnSkippyIfInitAlreadyFailed() {
        when(skippyAdapter.init(context)).thenReturn(false);
        instantiateStreamPlaya();
        reset(skippyAdapter);
        instantiateStreamPlaya();
        verifyZeroInteractions(skippyAdapter);
    }

    @Test
    public void playCallsPlayOnSkippyByDefault() {
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        verify(skippyAdapter).play(track);
    }

    @Test
    public void playSetsListenerOnSkippyByDefault() {
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        verify(skippyAdapter).setListener(streamPlayerWrapper);
    }

    @Test
    public void playUninterruptedCallsPlayOnSkippyByDefault() {
        instantiateStreamPlaya();
        streamPlayerWrapper.playUninterrupted(track);
        verify(skippyAdapter).playUninterrupted(track);
    }

    @Test
    public void playPlaysOnMediaPlayerIfSkippyLoadFailed() {
        when(skippyAdapter.init(context)).thenReturn(false);
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        verify(mediaPlayerAdapter).play(track);
    }

    @Test
    public void playCallsPlayOfflineOnSkippyIfTrackShouldBePlayedOffline() {
        when(offlinePlaybackOps.shouldPlayOffline(track)).thenReturn(true);
        instantiateStreamPlaya();

        streamPlayerWrapper.play(track);

        verify(skippyAdapter).playOffline(track, 0);
    }

    @Test
    public void playCallsPlayOfflineOnSkippyWithResumeTimeIfTrackIsAvailableOfflineAndNotMarkedForRemoval() {
        when(offlinePlaybackOps.shouldPlayOffline(track)).thenReturn(true);
        instantiateStreamPlaya();

        streamPlayerWrapper.play(track, 123);

        verify(skippyAdapter).playOffline(track, 123);
    }

    @Test
    public void playCallsPlayOnMediaPlayerIfTrackAvailableOfflineAndSkippyFailedToInitialize() {
        when(skippyAdapter.init(context)).thenReturn(false);
        instantiateStreamPlaya();

        streamPlayerWrapper.play(track);

        verify(mediaPlayerAdapter).play(track);
    }

    @Test
    public void playCallsPlayOnSkippyPlayerIfTrackShouldNotBePlayedOffline() {
        when(offlinePlaybackOps.shouldPlayOffline(track)).thenReturn(false);
        instantiateStreamPlaya();

        streamPlayerWrapper.play(track);

        verify(skippyAdapter).play(track);
    }

    @Test
    public void playCallsPlayOnSkippyIfTrackShouldNotBePlayedOffline() {
        when(offlinePlaybackOps.shouldPlayOffline(track)).thenReturn(false);
        instantiateStreamPlaya();

        streamPlayerWrapper.play(track);

        verify(skippyAdapter).play(track);
    }

    @Test
    public void startBufferingModeCallsStateChangeWithUrn() {
        instantiateStreamPlaya();
        streamPlayerWrapper.startBufferingMode(trackUrn);

        final Player.StateTransition expected = new Player.StateTransition(Player.PlayerState.BUFFERING, Player.Reason.NONE, trackUrn);
        verify(playerListener).onPlaystateChanged(expected);
    }

    @Test
    public void mediaPlayerIsStoppedWhenStartingBufferingMode() {
        instantiateStreamPlaya();
        fallBackToMediaPlayer();
        streamPlayerWrapper.startBufferingMode(trackUrn);
        verify(mediaPlayerAdapter).stopForTrackTransition();
    }

    @Test
    public void mediaPlayerIsStoppedWhenStartingBufferingModeBeforeSwitchingToSkippy() {
        instantiateStreamPlaya();
        fallBackToMediaPlayer();
        streamPlayerWrapper.startBufferingMode(trackUrn);
        verify(mediaPlayerAdapter).stopForTrackTransition();
    }

    @Test
    public void mediaPlayerListenerSetToNullWhenStartingBufferingMode() {
        instantiateStreamPlaya();
        fallBackToMediaPlayer();
        streamPlayerWrapper.startBufferingMode(trackUrn);
        verify(mediaPlayerAdapter).setListener(null);
    }

    @Test
    public void playSetsListenerToSkippy() {
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        verify(skippyAdapter).setListener(streamPlayerWrapper);
    }

    @Test
    public void skippyPlayerIsStoppedWhenBufferingModeStarted() {
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        streamPlayerWrapper.startBufferingMode(trackUrn);
        verify(skippyAdapter).stopForTrackTransition();
    }

    @Test
    public void skippyPlayerListenerIsSetToNullWhenBufferingModeStarted() {
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        streamPlayerWrapper.startBufferingMode(trackUrn);
        verify(skippyAdapter).setListener(null);
    }

    @Test
    public void resumeCallsResumeOnMediaPlayer() {
        instantiateStreamPlaya();
        fallBackToMediaPlayer();
        streamPlayerWrapper.resume();
        verify(mediaPlayerAdapter).resume();
    }

    @Test
    public void pauseCallsPauseOnMediaPlayer() {
        instantiateStreamPlaya();
        fallBackToMediaPlayer();
        streamPlayerWrapper.pause();
        verify(mediaPlayerAdapter).pause();
    }

    @Test
    public void seekCallsSeekOnMediaPlayer() {
        instantiateStreamPlaya();
        fallBackToMediaPlayer();
        streamPlayerWrapper.seek(100, true);
        verify(mediaPlayerAdapter).seek(100, true);
    }

    @Test
    public void getProgressCallsGetProgressOnMediaPlayer() {
        instantiateStreamPlaya();
        fallBackToMediaPlayer();
        streamPlayerWrapper.getProgress();
        verify(mediaPlayerAdapter).getProgress();
    }

    @Test
    public void setVolumeCallsSetVolumeOnMediaPlayer() {
        instantiateStreamPlaya();
        fallBackToMediaPlayer();
        streamPlayerWrapper.setVolume(3.0f);
        verify(mediaPlayerAdapter).setVolume(3.0f);
    }

    @Test
    public void stopCallsStopOnMediaPlayer() {
        instantiateStreamPlaya();
        fallBackToMediaPlayer();
        streamPlayerWrapper.stop();
        verify(mediaPlayerAdapter).stop();
    }

    @Test
    public void isSeekableReturnsMediaPlayerIsSeekable() {
        instantiateStreamPlaya();
        fallBackToMediaPlayer();
        when(mediaPlayerAdapter.isSeekable()).thenReturn(true);
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
    public void destroyCallsDestroyOnBothPlayers() {
        instantiateStreamPlaya();
        streamPlayerWrapper.destroy();
        verify(mediaPlayerAdapter).destroy();
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
        streamPlayerWrapper.play(track);
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(true);

        streamPlayerWrapper.onPlaystateChanged(new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.ERROR_FAILED, track.get(TrackProperty.URN)));
        verify(mediaPlayerAdapter).play(track, 123L);
    }

    @Test
    public void doesNotAutoRetryLastPlayOnMediaPlayerIfSkippyErrorsWithForbidden() {
        instantiateStreamPlaya();
        when(skippyAdapter.getProgress()).thenReturn(123L);
        streamPlayerWrapper.play(track);
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(true);

        streamPlayerWrapper.onPlaystateChanged(new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.ERROR_FORBIDDEN, track.get(TrackProperty.URN)));
        verify(mediaPlayerAdapter, never()).play(any(PropertySet.class), anyLong());
    }

    @Test
    public void doesNotAutoRetryLastPlayOnMediaPlayerIfSkippyErrorsWithNotFound() {
        instantiateStreamPlaya();
        when(skippyAdapter.getProgress()).thenReturn(123L);
        streamPlayerWrapper.play(track);
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(true);

        streamPlayerWrapper.onPlaystateChanged(new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.ERROR_NOT_FOUND, track.get(TrackProperty.URN)));
        verify(mediaPlayerAdapter, never()).play(any(PropertySet.class), anyLong());
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
        assertThat(streamPlayerWrapper.getLastStateTransition().getNewState()).isEqualTo(Player.PlayerState.IDLE);
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
    public void playbackHasPausedReturnTrueIfLastStateTransitionIsIdleNone() {
        instantiateStreamPlaya();
        streamPlayerWrapper.onPlaystateChanged(TestPlayStates.idle());
        assertThat(streamPlayerWrapper.playbackHasPaused()).isTrue();
    }

    @Test
    public void shouldNotDestroySkippyIfInitialisationFailed() {
        when(skippyAdapter.init(context)).thenReturn(false);
        instantiateStreamPlaya();
        streamPlayerWrapper.destroy();
        verify(skippyAdapter, never()).destroy();
    }

    private void fallBackToMediaPlayer() {
        streamPlayerWrapper.play(track);
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(true);
        streamPlayerWrapper.onPlaystateChanged(new Player.StateTransition(Player.PlayerState.IDLE, Player.Reason.ERROR_FAILED, track.get(TrackProperty.URN)));
    }

    private void startPlaybackOnSkippy() {
        streamPlayerWrapper.play(track);
    }
}
