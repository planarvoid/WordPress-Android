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

    private static final int MAX_CONSECUTIVE_SKIPPY_PLAYS = 2;
    private static final int MAX_CONSECUTIVE_MP_PLAYS = 4;

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
    public void tearDown(){
        StreamPlaya.skippyFailedToInitialize = false;
    }

    private void instantiateStreamPlaya() {
        streamPlayerWrapper = new StreamPlaya(context, mediaPlayerAdapter, skippyAdapter, bufferingPlaya, playerSwitcherInfo, offlinePlaybackOps, networkConnectionHelper);
        streamPlayerWrapper.setListener(playaListener);
    }

    @Test
    public void initCallsInitOnSkippy() throws Exception {
        instantiateStreamPlaya();
        verify(skippyAdapter).init(context);
    }

    @Test
    public void initDoesNotCallInitOnSkippyIfInitAlreadyFailed() throws Exception {
        when(skippyAdapter.init(context)).thenReturn(false);
        instantiateStreamPlaya();
        reset(skippyAdapter);
        instantiateStreamPlaya();
        verifyZeroInteractions(skippyAdapter);
    }

    @Test
    public void playCallsPlayOnSkippyByDefault() throws Exception {
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        verify(skippyAdapter).play(track);
    }

    @Test
    public void playSetsListenerOnSkippyByDefault() throws Exception {
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        verify(skippyAdapter).setListener(streamPlayerWrapper);
    }

    @Test
    public void playUninterruptedCallsPlayOnSkippyByDefault() throws Exception {
        instantiateStreamPlaya();
        streamPlayerWrapper.playUninterrupted(track);
        verify(skippyAdapter).playUninterrupted(track);
    }

    @Test
    public void playCallsPlayOnMediaPlayerIfForced() throws Exception {
        when(playerSwitcherInfo.shouldForceMediaPlayer()).thenReturn(true);
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        verify(mediaPlayerAdapter).play(track);
    }

    @Test
    public void playUninterruptedCallsPlayOnMediaPlayerIfForced() throws Exception {
        when(playerSwitcherInfo.shouldForceMediaPlayer()).thenReturn(true);
        instantiateStreamPlaya();
        streamPlayerWrapper.playUninterrupted(track);
        verify(mediaPlayerAdapter).playUninterrupted(track);
    }

    @Test
    public void playPlaysOnMediaPlayerIfSkippyLoadFailed() throws Exception {
        when(skippyAdapter.init(context)).thenReturn(false);
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        verify(mediaPlayerAdapter).play(track);
    }

    @Test
    public void playCallsPlayOfflineOnSkippyIfTrackShouldBePlayedOffline() throws Exception {
        when(offlinePlaybackOps.shouldPlayOffline(track)).thenReturn(true);
        instantiateStreamPlaya();

        streamPlayerWrapper.play(track);

        verify(skippyAdapter).playOffline(track, 0);
    }

    @Test
    public void playCallsPlayOfflineOnSkippyWithResumeTimeIfTrackIsAvailableOfflineAndNotMarkedForRemoval() throws Exception {
        when(offlinePlaybackOps.shouldPlayOffline(track)).thenReturn(true);
        instantiateStreamPlaya();

        streamPlayerWrapper.play(track, 123);

        verify(skippyAdapter).playOffline(track, 123);
    }

    @Test
    public void playCallsPlayOnMediaPlayerIfTrackAvailableOfflineAndSkippyFailedToInitialize() throws Exception {
        when(skippyAdapter.init(context)).thenReturn(false);
        instantiateStreamPlaya();

        streamPlayerWrapper.play(track);

        verify(mediaPlayerAdapter).play(track);
    }

    @Test
    public void playCallsPlayOnSkippyPlayerIfTrackShouldNotBePlayedOffline() throws Exception {
        when(offlinePlaybackOps.shouldPlayOffline(track)).thenReturn(false);
        instantiateStreamPlaya();

        streamPlayerWrapper.play(track);

        verify(skippyAdapter).play(track);
    }

    @Test
    public void playCallsPlayOnSkippyIfTrackShouldNotBePlayedOffline() throws Exception {
        when(offlinePlaybackOps.shouldPlayOffline(track)).thenReturn(false);
        instantiateStreamPlaya();

        streamPlayerWrapper.play(track);

        verify(skippyAdapter).play(track);
    }

    @Test
    public void mediaPlayerIsStoppedPlayIsCalledAndSkippyIsConfigured() throws Exception {
        when(playerSwitcherInfo.shouldForceMediaPlayer()).thenReturn(true);
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        when(playerSwitcherInfo.shouldForceMediaPlayer()).thenReturn(false);
        streamPlayerWrapper.play(track);
        verify(mediaPlayerAdapter).stopForTrackTransition();
    }

    @Test
    public void skippyIsStoppedPlayIsCalledAndMediaPlayerIsConfigured() throws Exception {
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        when(playerSwitcherInfo.shouldForceMediaPlayer()).thenReturn(true);
        streamPlayerWrapper.play(track);
        verify(skippyAdapter).stopForTrackTransition();
    }

    @Test
    public void startBufferingModeCallsStateChangeWithUrn() throws Exception {
        instantiateStreamPlaya();
        streamPlayerWrapper.startBufferingMode(Urn.forTrack(1L));

        final Playa.StateTransition expected = new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE, Urn.forTrack(1L));
        verify(playaListener).onPlaystateChanged(expected);
    }

    @Test
    public void mediaPlayerIsStoppedWhenStartingBufferingMode() throws Exception {
        when(playerSwitcherInfo.shouldForceMediaPlayer()).thenReturn(true);
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        streamPlayerWrapper.startBufferingMode(Urn.forTrack(1L));
        verify(mediaPlayerAdapter).stopForTrackTransition();
    }

    @Test
    public void mediaPlayerIsStoppedWhenStartingBufferingModeBeforeSwitchingToSkippy() throws Exception {
        when(playerSwitcherInfo.shouldForceMediaPlayer()).thenReturn(true);
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        when(playerSwitcherInfo.shouldForceMediaPlayer()).thenReturn(false);
        streamPlayerWrapper.startBufferingMode(Urn.forTrack(1L));
        verify(mediaPlayerAdapter).stopForTrackTransition();
    }

    @Test
    public void mediaPlayerListenerSetToNullWhenStartingBufferingMode() throws Exception {
        when(playerSwitcherInfo.shouldForceMediaPlayer()).thenReturn(true);
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        when(playerSwitcherInfo.shouldForceMediaPlayer()).thenReturn(false);
        streamPlayerWrapper.startBufferingMode(Urn.forTrack(1L));
        verify(mediaPlayerAdapter).setListener(null);
    }

    @Test
    public void playSetsListenerToSkippy() throws Exception {
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        verify(skippyAdapter).setListener(streamPlayerWrapper);
    }

    @Test
    public void skippyPlayerIsStoppedWhenBufferingModeStarted() throws Exception {
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        streamPlayerWrapper.startBufferingMode(Urn.forTrack(1L));
        verify(skippyAdapter).stopForTrackTransition();
    }

    @Test
    public void skippyPlayerListenerIsSetToNullWhenBufferingModeStarted() throws Exception {
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        streamPlayerWrapper.startBufferingMode(Urn.forTrack(1L));
        verify(skippyAdapter).setListener(null);
    }

    @Test
    public void resumeCallsResumeOnMediaPlayer() throws Exception {
        instantiateStreamPlaya();
        startPlaybackOnMediaPlayer();
        streamPlayerWrapper.resume();
        verify(mediaPlayerAdapter).resume();
    }

    @Test
    public void pauseCallsPauseOnMediaPlayer() throws Exception {
        instantiateStreamPlaya();
        startPlaybackOnMediaPlayer();
        streamPlayerWrapper.pause();
        verify(mediaPlayerAdapter).pause();
    }

    @Test
    public void seekCallsSeekOnMediaPlayer() throws Exception {
        instantiateStreamPlaya();
        startPlaybackOnMediaPlayer();
        streamPlayerWrapper.seek(100, true);
        verify(mediaPlayerAdapter).seek(100, true);
    }

    @Test
    public void getProgressCallsGetProgressOnMediaPlayer() throws Exception {
        instantiateStreamPlaya();
        startPlaybackOnMediaPlayer();
        streamPlayerWrapper.getProgress();
        verify(mediaPlayerAdapter).getProgress();
    }

    @Test
    public void setVolumeCallsSetVolumeOnMediaPlayer() throws Exception {
        instantiateStreamPlaya();
        startPlaybackOnMediaPlayer();
        streamPlayerWrapper.setVolume(3.0f);
        verify(mediaPlayerAdapter).setVolume(3.0f);
    }

    @Test
    public void stopCallsStopOnMediaPlayer() throws Exception {
        instantiateStreamPlaya();
        startPlaybackOnMediaPlayer();
        streamPlayerWrapper.stop();
        verify(mediaPlayerAdapter).stop();
    }

    @Test
    public void isSeekableReturnsMediaPlayerIsSeekable() throws Exception {
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
    public void resumeCallsResumeOnSkippyPlayer() throws Exception {
        instantiateStreamPlaya();
        startPlaybackOnSkippy();
        streamPlayerWrapper.resume();
        verify(skippyAdapter).resume();
    }

    @Test
    public void pauseCallsPauseOnSkippy() throws Exception {
        instantiateStreamPlaya();
        startPlaybackOnSkippy();
        streamPlayerWrapper.pause();
        verify(skippyAdapter).pause();
    }

    @Test
    public void seekCallsSeekOnSkippy() throws Exception {
        instantiateStreamPlaya();
        startPlaybackOnSkippy();
        streamPlayerWrapper.seek(100, true);
        verify(skippyAdapter).seek(100, true);
    }

    @Test
    public void getProgressCallsGetProgressOnSkippy() throws Exception {
        instantiateStreamPlaya();
        startPlaybackOnSkippy();
        streamPlayerWrapper.getProgress();
        verify(skippyAdapter).getProgress();
    }

    @Test
    public void setVolumeCallsSetVolumeOnSkippy() throws Exception {
        instantiateStreamPlaya();
        startPlaybackOnSkippy();
        streamPlayerWrapper.setVolume(3.0f);
        verify(skippyAdapter).setVolume(3.0f);
    }

    @Test
    public void stopCallsStopOnSkippy() throws Exception {
        instantiateStreamPlaya();
        startPlaybackOnSkippy();
        streamPlayerWrapper.stop();
        verify(skippyAdapter).stop();
    }

    @Test
    public void isSeekableReturnsSkippyIsSeekable() throws Exception {
        instantiateStreamPlaya();
        startPlaybackOnSkippy();
        when(skippyAdapter.isSeekable()).thenReturn(true);
        expect(streamPlayerWrapper.isSeekable()).toBeTrue();
    }

    @Test
    public void destroyCallsDestroyOnBothPlayers() throws Exception {
        instantiateStreamPlaya();
        streamPlayerWrapper.destroy();
        verify(mediaPlayerAdapter).destroy();
        verify(skippyAdapter).destroy();
    }

    @Test(expected = NullPointerException.class)
    public void onPlaystateChangeDoesThrowsNPEWithSuccessStateAndNoListener(){
        instantiateStreamPlaya();
        streamPlayerWrapper.setListener(null);
        streamPlayerWrapper.onPlaystateChanged(TestPlayStates.buffering());
    }

    @Test
    public void onPlaystateChangedPassesSuccessEventToListener() throws Exception {
        instantiateStreamPlaya();
        streamPlayerWrapper.setListener(playaListener);

        final Playa.StateTransition transition = TestPlayStates.buffering();
        streamPlayerWrapper.onPlaystateChanged(transition);
        verify(playaListener).onPlaystateChanged(transition);
    }

    @Test
    public void autoRetriesLastPlayOnMediaPlayerIfSkippyErrorsWhileConnectedToInternet() throws Exception {
        instantiateStreamPlaya();
        when(skippyAdapter.getProgress()).thenReturn(123L);
        streamPlayerWrapper.play(track);
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(true);

        streamPlayerWrapper.onPlaystateChanged(new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_FAILED, track.get(TrackProperty.URN)));
        verify(mediaPlayerAdapter).play(track, 123L);
    }

    @Test
    public void doesNotAutoRetryLastPlayOnMediaPlayerIfSkippyErrorsWithForbidden() throws Exception {
        instantiateStreamPlaya();
        when(skippyAdapter.getProgress()).thenReturn(123L);
        streamPlayerWrapper.play(track);
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(true);

        streamPlayerWrapper.onPlaystateChanged(new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_FORBIDDEN, track.get(TrackProperty.URN)));
        verify(mediaPlayerAdapter, never()).play(any(PropertySet.class), anyLong());
    }

    @Test
    public void doesNotAutoRetryLastPlayOnMediaPlayerIfSkippyErrorsWithNotFound() throws Exception {
        instantiateStreamPlaya();
        when(skippyAdapter.getProgress()).thenReturn(123L);
        streamPlayerWrapper.play(track);
        when(networkConnectionHelper.isNetworkConnected()).thenReturn(true);

        streamPlayerWrapper.onPlaystateChanged(new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_NOT_FOUND, track.get(TrackProperty.URN)));
        verify(mediaPlayerAdapter, never()).play(any(PropertySet.class), anyLong());
    }

    @Test
    public void onPlaystateChangedPassesErrorEventToListenerWhenNotConnectedToInternet() throws Exception {
        instantiateStreamPlaya();
        startPlaybackOnSkippy();
        when(skippyAdapter.getProgress()).thenReturn(1L);

        final Playa.StateTransition stateTransition = new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_NOT_FOUND, Urn.forTrack(123L));
        streamPlayerWrapper.onPlaystateChanged(stateTransition);
        verify(playaListener).onPlaystateChanged(stateTransition);
    }

    @Test
    public void requestsFocusOnListener() throws Exception {
        instantiateStreamPlaya();
        streamPlayerWrapper.requestAudioFocus();
        verify(playaListener).requestAudioFocus();
    }

    @Test(expected = NullPointerException.class)
    public void requestsFocusThrowsNPEWithNoListener() throws Exception {
        instantiateStreamPlaya();
        streamPlayerWrapper.setListener(null);
        streamPlayerWrapper.requestAudioFocus();
    }

    @Test
    public void getStateReturnsIdleByDefault() throws Exception {
        instantiateStreamPlaya();
        expect(streamPlayerWrapper.getState()).toBe(Playa.PlayaState.IDLE);
    }

    @Test
    public void getStateReturnsLastState() throws Exception {
        instantiateStreamPlaya();
        streamPlayerWrapper.onPlaystateChanged(TestPlayStates.buffering());
        expect(streamPlayerWrapper.getState()).toBe(Playa.PlayaState.BUFFERING);
    }

    @Test
    public void getLastStateTransitionReturnsIdleNothingByDefault() throws Exception {
        instantiateStreamPlaya();
        expect(streamPlayerWrapper.getLastStateTransition()).toEqual(TestPlayStates.idleDefault());
    }

    @Test
    public void getLastStateTransitionReturnsLastTransition() throws Exception {
        instantiateStreamPlaya();
        final Playa.StateTransition stateTransition = TestPlayStates.buffering();
        streamPlayerWrapper.onPlaystateChanged(stateTransition);
        expect(streamPlayerWrapper.getLastStateTransition()).toEqual(stateTransition);
    }

    @Test
    public void isPlayingReturnsIsPlayingFromLastTransition() throws Exception {
        instantiateStreamPlaya();
        streamPlayerWrapper.onPlaystateChanged(TestPlayStates.buffering());
        expect(streamPlayerWrapper.isPlaying()).toEqual(true);
    }

    @Test
    public void isPlayerPlayingReturnsIsPlayerPlayingFromLastTransition() throws Exception {
        instantiateStreamPlaya();
        streamPlayerWrapper.onPlaystateChanged(TestPlayStates.playing());
        expect(streamPlayerWrapper.isPlaying()).toEqual(true);
    }

    @Test
    public void isBufferingReturnsIsPlayerPlayingFromLastTransition() throws Exception {
        instantiateStreamPlaya();
        streamPlayerWrapper.onPlaystateChanged(TestPlayStates.buffering());
        expect(streamPlayerWrapper.isPlaying()).toEqual(true);
    }

    @Test
    public void playbackHasPausedReturnTrueIfLastStateTransitionIsIdleNone() throws Exception {
        instantiateStreamPlaya();
        streamPlayerWrapper.onPlaystateChanged(TestPlayStates.idle());
        expect(streamPlayerWrapper.playbackHasPaused()).toEqual(true);
    }

    @Test
    public void shouldNotDestroySkippyIfInitialisationFailed(){
        when(skippyAdapter.init(context)).thenReturn(false);
        instantiateStreamPlaya();
        streamPlayerWrapper.destroy();
        verify(skippyAdapter, never()).destroy();
    }

    private void startPlaybackOnSkippy() {
        streamPlayerWrapper.play(track);
    }
}
