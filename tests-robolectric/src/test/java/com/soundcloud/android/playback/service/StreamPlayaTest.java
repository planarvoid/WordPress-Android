package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflinePlaybackOperations;
import com.soundcloud.android.playback.service.mediaplayer.MediaPlayerAdapter;
import com.soundcloud.android.playback.service.skippy.SkippyAdapter;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.propeller.PropertySet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Context;

@RunWith(SoundCloudTestRunner.class)
public class StreamPlayaTest {

    private final Urn trackUrn = Urn.forTrack(1L);

    private StreamPlaya streamPlayerWrapper;
    @Mock private Context context;
    @Mock private MediaPlayerAdapter mediaPlayerAdapter;
    @Mock private SkippyAdapter skippyAdapter;
    @Mock private BufferingPlaya bufferingPlaya;
    @Mock private Playa.PlayaListener playaListener;
    @Mock private StreamPlaya.PlayerSwitcherInfo playerSwitcherInfo;
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
        StreamPlaya.skippyFailedToInitialize = false;
    }

    private void instantiateStreamPlaya() {
        streamPlayerWrapper = new StreamPlaya(context, mediaPlayerAdapter, skippyAdapter, bufferingPlaya, playerSwitcherInfo, offlinePlaybackOps, networkConnectionHelper);
        streamPlayerWrapper.setListener(playaListener);
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
    public void playCallsPlayOnMediaPlayerIfForced() {
        when(playerSwitcherInfo.shouldForceMediaPlayer()).thenReturn(true);
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        verify(mediaPlayerAdapter).play(track);
    }

    @Test
    public void playUninterruptedCallsPlayOnMediaPlayerIfForced() {
        when(playerSwitcherInfo.shouldForceMediaPlayer()).thenReturn(true);
        instantiateStreamPlaya();
        streamPlayerWrapper.playUninterrupted(track);
        verify(mediaPlayerAdapter).playUninterrupted(track);
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
    public void playCallsPlayOfflineOnSkippyEvenIfMediaPlayerIsForced() {
        when(offlinePlaybackOps.shouldPlayOffline(track)).thenReturn(true);
        when(playerSwitcherInfo.shouldForceMediaPlayer()).thenReturn(true);

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
    public void mediaPlayerIsStoppedPlayIsCalledAndSkippyIsConfigured() {
        when(playerSwitcherInfo.shouldForceMediaPlayer()).thenReturn(true);
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        when(playerSwitcherInfo.shouldForceMediaPlayer()).thenReturn(false);
        streamPlayerWrapper.play(track);
        verify(mediaPlayerAdapter).stopForTrackTransition();
    }

    @Test
    public void skippyIsStoppedPlayIsCalledAndMediaPlayerIsConfigured() {
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        when(playerSwitcherInfo.shouldForceMediaPlayer()).thenReturn(true);
        streamPlayerWrapper.play(track);
        verify(skippyAdapter).stopForTrackTransition();
    }

    @Test
    public void startBufferingModeCallsStateChangeWithUrn() {
        instantiateStreamPlaya();
        streamPlayerWrapper.startBufferingMode(trackUrn);

        final Playa.StateTransition expected = new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE, trackUrn);
        verify(playaListener).onPlaystateChanged(expected);
    }

    @Test
    public void mediaPlayerIsStoppedWhenStartingBufferingMode() {
        when(playerSwitcherInfo.shouldForceMediaPlayer()).thenReturn(true);
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        streamPlayerWrapper.startBufferingMode(trackUrn);
        verify(mediaPlayerAdapter).stopForTrackTransition();
    }

    @Test
    public void mediaPlayerIsStoppedWhenStartingBufferingModeBeforeSwitchingToSkippy() {
        when(playerSwitcherInfo.shouldForceMediaPlayer()).thenReturn(true);
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        when(playerSwitcherInfo.shouldForceMediaPlayer()).thenReturn(false);
        streamPlayerWrapper.startBufferingMode(trackUrn);
        verify(mediaPlayerAdapter).stopForTrackTransition();
    }

    @Test
    public void mediaPlayerListenerSetToNullWhenStartingBufferingMode() {
        when(playerSwitcherInfo.shouldForceMediaPlayer()).thenReturn(true);
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        when(playerSwitcherInfo.shouldForceMediaPlayer()).thenReturn(false);
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
        startPlaybackOnMediaPlayer();
        streamPlayerWrapper.resume();
        verify(mediaPlayerAdapter).resume();
    }

    @Test
    public void pauseCallsPauseOnMediaPlayer() {
        instantiateStreamPlaya();
        startPlaybackOnMediaPlayer();
        streamPlayerWrapper.pause();
        verify(mediaPlayerAdapter).pause();
    }

    @Test
    public void seekCallsSeekOnMediaPlayer() {
        instantiateStreamPlaya();
        startPlaybackOnMediaPlayer();
        streamPlayerWrapper.seek(100, true);
        verify(mediaPlayerAdapter).seek(100, true);
    }

    @Test
    public void getProgressCallsGetProgressOnMediaPlayer() {
        instantiateStreamPlaya();
        startPlaybackOnMediaPlayer();
        streamPlayerWrapper.getProgress();
        verify(mediaPlayerAdapter).getProgress();
    }

    @Test
    public void setVolumeCallsSetVolumeOnMediaPlayer() {
        instantiateStreamPlaya();
        startPlaybackOnMediaPlayer();
        streamPlayerWrapper.setVolume(3.0f);
        verify(mediaPlayerAdapter).setVolume(3.0f);
    }

    @Test
    public void stopCallsStopOnMediaPlayer() {
        instantiateStreamPlaya();
        startPlaybackOnMediaPlayer();
        streamPlayerWrapper.stop();
        verify(mediaPlayerAdapter).stop();
    }

    @Test
    public void isSeekableReturnsMediaPlayerIsSeekable() {
        instantiateStreamPlaya();
        startPlaybackOnMediaPlayer();
        when(mediaPlayerAdapter.isSeekable()).thenReturn(true);
        expect(streamPlayerWrapper.isSeekable()).toBeTrue();
    }

    private void startPlaybackOnMediaPlayer() {
        instantiateStreamPlaya();
        when(playerSwitcherInfo.shouldForceMediaPlayer()).thenReturn(true);
        streamPlayerWrapper.play(track);
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
        expect(streamPlayerWrapper.isSeekable()).toBeTrue();
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
        streamPlayerWrapper.setListener(playaListener);

        final Playa.StateTransition transition = TestPlayStates.buffering();
        streamPlayerWrapper.onPlaystateChanged(transition);
        verify(playaListener).onPlaystateChanged(transition);
    }

    @Test
    public void autoRetriesLastPlayOnMediaPlayerIfSkippyErrorsWhileConnectedToInternet() {
        instantiateStreamPlaya();
        when(skippyAdapter.getProgress()).thenReturn(123L);
        streamPlayerWrapper.play(track);
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(true);

        streamPlayerWrapper.onPlaystateChanged(new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_FAILED, track.get(TrackProperty.URN)));
        verify(mediaPlayerAdapter).play(track, 123L);
    }

    @Test
    public void doesNotAutoRetryLastPlayOnMediaPlayerIfSkippyErrorsWithForbidden() {
        instantiateStreamPlaya();
        when(skippyAdapter.getProgress()).thenReturn(123L);
        streamPlayerWrapper.play(track);
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(true);

        streamPlayerWrapper.onPlaystateChanged(new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_FORBIDDEN, track.get(TrackProperty.URN)));
        verify(mediaPlayerAdapter, never()).play(any(PropertySet.class), anyLong());
    }

    @Test
    public void doesNotAutoRetryLastPlayOnMediaPlayerIfSkippyErrorsWithNotFound() {
        instantiateStreamPlaya();
        when(skippyAdapter.getProgress()).thenReturn(123L);
        streamPlayerWrapper.play(track);
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(true);

        streamPlayerWrapper.onPlaystateChanged(new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_NOT_FOUND, track.get(TrackProperty.URN)));
        verify(mediaPlayerAdapter, never()).play(any(PropertySet.class), anyLong());
    }

    @Test
    public void onPlaystateChangedPassesErrorEventToListenerWhenNotConnectedToInternet() {
        instantiateStreamPlaya();
        startPlaybackOnSkippy();
        when(skippyAdapter.getProgress()).thenReturn(1L);

        final Playa.StateTransition stateTransition = new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_NOT_FOUND, Urn.forTrack(123L));
        streamPlayerWrapper.onPlaystateChanged(stateTransition);
        verify(playaListener).onPlaystateChanged(stateTransition);
    }

    @Test
    public void requestsFocusOnListener() {
        instantiateStreamPlaya();
        streamPlayerWrapper.requestAudioFocus();
        verify(playaListener).requestAudioFocus();
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
        expect(streamPlayerWrapper.getState()).toBe(Playa.PlayaState.IDLE);
    }

    @Test
    public void getStateReturnsLastState() {
        instantiateStreamPlaya();
        streamPlayerWrapper.onPlaystateChanged(TestPlayStates.buffering());
        expect(streamPlayerWrapper.getState()).toBe(Playa.PlayaState.BUFFERING);
    }

    @Test
    public void getLastStateTransitionReturnsIdleNothingByDefault() {
        instantiateStreamPlaya();
        expect(streamPlayerWrapper.getLastStateTransition()).toEqual(TestPlayStates.idleDefault());
    }

    @Test
    public void getLastStateTransitionReturnsLastTransition() {
        instantiateStreamPlaya();
        final Playa.StateTransition stateTransition = TestPlayStates.buffering();
        streamPlayerWrapper.onPlaystateChanged(stateTransition);
        expect(streamPlayerWrapper.getLastStateTransition()).toEqual(stateTransition);
    }

    @Test
    public void isPlayingReturnsIsPlayingFromLastTransition() {
        instantiateStreamPlaya();
        streamPlayerWrapper.onPlaystateChanged(TestPlayStates.buffering());
        expect(streamPlayerWrapper.isPlaying()).toEqual(true);
    }

    @Test
    public void isPlayerPlayingReturnsIsPlayerPlayingFromLastTransition() {
        instantiateStreamPlaya();
        streamPlayerWrapper.onPlaystateChanged(TestPlayStates.playing());
        expect(streamPlayerWrapper.isPlaying()).toEqual(true);
    }

    @Test
    public void isBufferingReturnsIsPlayerPlayingFromLastTransition() {
        instantiateStreamPlaya();
        streamPlayerWrapper.onPlaystateChanged(TestPlayStates.buffering());
        expect(streamPlayerWrapper.isPlaying()).toEqual(true);
    }

    @Test
    public void playbackHasPausedReturnTrueIfLastStateTransitionIsIdleNone() {
        instantiateStreamPlaya();
        streamPlayerWrapper.onPlaystateChanged(TestPlayStates.idle());
        expect(streamPlayerWrapper.playbackHasPaused()).toEqual(true);
    }

    @Test
    public void shouldNotDestroySkippyIfInitialisationFailed() {
        when(skippyAdapter.init(context)).thenReturn(false);
        instantiateStreamPlaya();
        streamPlayerWrapper.destroy();
        verify(skippyAdapter, never()).destroy();
    }

    private void startPlaybackOnSkippy() {
        streamPlayerWrapper.play(track);
    }
}
