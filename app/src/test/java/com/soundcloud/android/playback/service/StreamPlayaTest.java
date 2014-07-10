package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.mediaplayer.MediaPlayerAdapter;
import com.soundcloud.android.playback.service.skippy.SkippyAdapter;
import com.soundcloud.android.preferences.DeveloperPreferences;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Context;
import android.content.SharedPreferences;

@RunWith(SoundCloudTestRunner.class)
public class StreamPlayaTest {

    private static final int MAX_CONSECUTIVE_SKIPPY_PLAYS = 2;
    private static final int MAX_CONSECUTIVE_MP_PLAYS = 4;

    private StreamPlaya streamPlayerWrapper;
    @Mock
    private Context context;
    @Mock
    private SharedPreferences sharedPreferences;
    @Mock
    private SharedPreferences.Editor sharedPreferencesEditor;
    @Mock
    private MediaPlayerAdapter mediaPlayerAdapter;
    @Mock
    private SkippyAdapter skippyAdapter;
    @Mock
    private BufferingPlaya bufferingPlaya;
    @Mock
    private Playa.PlayaListener playaListener;
    @Mock
    private FeatureFlags featureFlags;
    @Mock
    private Track track;
    @Mock
    private StreamPlaya.PlayerSwitcherInfo playerSwitcherInfo;

    @Before
    public void setUp() throws Exception {
        when(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor);
        when(sharedPreferencesEditor.putInt(anyString(), anyInt())).thenReturn(sharedPreferencesEditor);
        when(skippyAdapter.init(context)).thenReturn(true);
        when(playerSwitcherInfo.getMaxConsecutiveMpPlays()).thenReturn(MAX_CONSECUTIVE_MP_PLAYS);
        when(playerSwitcherInfo.getMaxConsecutiveSkippyPlays()).thenReturn(MAX_CONSECUTIVE_SKIPPY_PLAYS);
    }

    @After
    public void tearDown(){
        StreamPlaya.skippyFailedToInitialize = false;
    }

    private void instantiateStreamPlaya() {
        streamPlayerWrapper = new StreamPlaya(context, sharedPreferences, mediaPlayerAdapter, skippyAdapter, bufferingPlaya, featureFlags, playerSwitcherInfo);
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
    public void playUrlCallsPlayUrlOnMediaPlayerByDefault() throws Exception {
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        verify(mediaPlayerAdapter).play(track);
    }

    @Test
    public void playUrlSetsListenerOnMediaPlayerByDefault() throws Exception {
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        verify(mediaPlayerAdapter).setListener(streamPlayerWrapper);
    }

    @Test
    public void playUrlCallsPlayUrlOnSkippyPlayerIfPreferenceSet() throws Exception {
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(DeveloperPreferences.DEV_FORCE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.play(track);
        verify(skippyAdapter).play(track);
    }

    @Test
    public void playUrlSetsPlayListenerOnSkippyPlayerIfPreferenceSet() throws Exception {
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(DeveloperPreferences.DEV_FORCE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.play(track);
        skippyAdapter.play(track);
        verify(skippyAdapter).setListener(streamPlayerWrapper);
    }

    @Test
    public void playUrlPlaysOnMediaPlayerIfSkippyEnabledButSkippyLoadFailed() throws Exception {
        when(skippyAdapter.init(context)).thenReturn(false);
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(DeveloperPreferences.DEV_FORCE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.play(track);
        verify(mediaPlayerAdapter).play(track);
    }

    @Test
    public void mediaPlayerIsStoppedPlayUrlIsCalledAndSkippyIsConfigured() throws Exception {
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        when(sharedPreferences.getBoolean(DeveloperPreferences.DEV_FORCE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.play(track);
        verify(mediaPlayerAdapter).stop();
    }

    @Test
    public void skippyIsStoppedPlayUrlIsCalledAndMediaPlayerIsConfigured() throws Exception {
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(DeveloperPreferences.DEV_FORCE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.play(track);
        when(sharedPreferences.getBoolean(DeveloperPreferences.DEV_FORCE_SKIPPY, false)).thenReturn(false);
        streamPlayerWrapper.play(track);
        verify(skippyAdapter).stop();
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
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        streamPlayerWrapper.startBufferingMode(Urn.forTrack(1L));
        verify(mediaPlayerAdapter).stop();
    }


    @Test
    public void mediaPlayerListenerSetToNullWhenStartingBufferingMode() throws Exception {
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        when(sharedPreferences.getBoolean(DeveloperPreferences.DEV_FORCE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.startBufferingMode(Urn.forTrack(1L));
        verify(mediaPlayerAdapter).setListener(null);
    }

    @Test
    public void playUrlSetsListenerToSkippy() throws Exception {
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(DeveloperPreferences.DEV_FORCE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.play(track);
        verify(skippyAdapter).setListener(streamPlayerWrapper);
    }

    @Test
    public void skippyPlayerIsStoppedWhenBufferingModeStarted() throws Exception {
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(DeveloperPreferences.DEV_FORCE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.play(track);
        streamPlayerWrapper.startBufferingMode(Urn.forTrack(1L));
        verify(skippyAdapter).stop();
    }

    @Test
    public void skippyPlayerListenerIsSetToNullWhenBufferingModeStarted() throws Exception {
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(DeveloperPreferences.DEV_FORCE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.play(track);
        streamPlayerWrapper.startBufferingMode(Urn.forTrack(1L));
        verify(skippyAdapter).setListener(null);
    }

    @Test
    public void playUrlUsesMediaPlayerWhenTotalNumberOfPlaysHasNotHitThreshhold() throws Exception {
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(DeveloperPreferences.DEV_FORCE_SKIPPY, false)).thenReturn(false);
        when(sharedPreferences.getInt(StreamPlaya.PLAYS_ON_CURRENT_PLAYER, 0)).thenReturn(MAX_CONSECUTIVE_SKIPPY_PLAYS - 1);
        streamPlayerWrapper.play(track);
        verify(mediaPlayerAdapter).play(track);
    }

    @Test
    public void playUrlIncreasesCountWhenTotalNumberOfPlayHasNotHitThreshhold() throws Exception {
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        streamPlayerWrapper.startBufferingMode(Urn.forTrack(1L));

        reset(sharedPreferencesEditor);
        when(sharedPreferencesEditor.putInt(anyString(), anyInt())).thenReturn(sharedPreferencesEditor);
        when(sharedPreferences.getBoolean(DeveloperPreferences.DEV_FORCE_SKIPPY, false)).thenReturn(false);
        when(sharedPreferences.getInt(StreamPlaya.PLAYS_ON_CURRENT_PLAYER, 0)).thenReturn(MAX_CONSECUTIVE_SKIPPY_PLAYS - 1);

        streamPlayerWrapper.play(track);
        verify(sharedPreferencesEditor).putInt(StreamPlaya.PLAYS_ON_CURRENT_PLAYER, MAX_CONSECUTIVE_SKIPPY_PLAYS);
        verify(sharedPreferencesEditor).apply();
    }

    @Test
    public void playUrlUsesSkippyWhenTotalPlaysEqualsThreshold() throws Exception {
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        streamPlayerWrapper.startBufferingMode(Urn.forTrack(1L));

        when(sharedPreferences.getBoolean(DeveloperPreferences.DEV_FORCE_SKIPPY, false)).thenReturn(false);
        when(sharedPreferences.getInt(StreamPlaya.PLAYS_ON_CURRENT_PLAYER, 0)).thenReturn(MAX_CONSECUTIVE_MP_PLAYS);
        streamPlayerWrapper.play(track);
        verify(skippyAdapter).play(track);
    }

    @Test
    public void playUrlResetsCountWhenTotalPlaysEqualsThreshold() throws Exception {
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        streamPlayerWrapper.startBufferingMode(Urn.forTrack(1L));

        reset(sharedPreferencesEditor);
        when(sharedPreferencesEditor.putInt(anyString(), anyInt())).thenReturn(sharedPreferencesEditor);
        when(sharedPreferences.getBoolean(DeveloperPreferences.DEV_FORCE_SKIPPY, false)).thenReturn(false);
        when(sharedPreferences.getInt(StreamPlaya.PLAYS_ON_CURRENT_PLAYER, 0)).thenReturn(MAX_CONSECUTIVE_MP_PLAYS);

        streamPlayerWrapper.play(track);
        verify(sharedPreferencesEditor).putInt(StreamPlaya.PLAYS_ON_CURRENT_PLAYER, 1);
    }

    @Test
    public void playUrlPlaysOnMediaPlayerTotalPlaysEqualsThresholdButSkippyLoadFailed() throws Exception {
        when(skippyAdapter.init(context)).thenReturn(false);
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(DeveloperPreferences.DEV_FORCE_SKIPPY, false)).thenReturn(false);
        when(sharedPreferences.getInt(StreamPlaya.PLAYS_ON_CURRENT_PLAYER, 0)).thenReturn(MAX_CONSECUTIVE_MP_PLAYS);
        streamPlayerWrapper.play(track);
        verify(mediaPlayerAdapter).play(track);
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
        when(sharedPreferences.getBoolean(DeveloperPreferences.DEV_FORCE_SKIPPY, false)).thenReturn(false);
        streamPlayerWrapper.play(track);
    }

    @Test
    public void isNotSeekablePastBufferReturnsMediaPlayerIsNotSeekablePastBuffer() throws Exception {
        instantiateStreamPlaya();
        startPlaybackOnMediaPlayer();
        when(mediaPlayerAdapter.isNotSeekablePastBuffer()).thenReturn(true);
        expect(streamPlayerWrapper.isNotSeekablePastBuffer()).toBeTrue();
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
    public void isNotSeekablePastBufferReturnsSkippyIsNotSeekablePastBuffer() throws Exception {
        instantiateStreamPlaya();
        startPlaybackOnSkippy();
        when(skippyAdapter.isNotSeekablePastBuffer()).thenReturn(true);
        expect(streamPlayerWrapper.isNotSeekablePastBuffer()).toBeTrue();
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
        final Playa.StateTransition transition = new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE);
        streamPlayerWrapper.onPlaystateChanged(transition);
    }

    @Test
    public void onPlaystateChangedPassesSuccessEventToListener() throws Exception {
        instantiateStreamPlaya();
        streamPlayerWrapper.setListener(playaListener);

        final Playa.StateTransition transition = new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE);
        streamPlayerWrapper.onPlaystateChanged(transition);
        verify(playaListener).onPlaystateChanged(transition);
    }

    @Test
    public void autoRetriesLastPlayOnMediaPlayerIfSkippyErrors() throws Exception {
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(DeveloperPreferences.DEV_FORCE_SKIPPY, false)).thenReturn(false);
        when(sharedPreferences.getInt(StreamPlaya.PLAYS_ON_CURRENT_PLAYER, 0)).thenReturn(MAX_CONSECUTIVE_MP_PLAYS);
        when(skippyAdapter.getProgress()).thenReturn(123L);
        streamPlayerWrapper.play(track);

        streamPlayerWrapper.onPlaystateChanged(new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_NOT_FOUND, track.getUrn()));
        verify(mediaPlayerAdapter).play(track, 123L);
    }

    @Test
    public void doesNotAutoRetrySkippyErrorIfSkippyModeEnabled() throws Exception {
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(DeveloperPreferences.DEV_FORCE_SKIPPY, false)).thenReturn(true);
        startPlaybackOnSkippy();
        streamPlayerWrapper.onPlaystateChanged(new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_NOT_FOUND, track.getUrn()));
        verify(mediaPlayerAdapter, never()).play(any(Track.class), anyLong());
    }

    @Test
    public void doesNotAutoRetrySkippyErrorIfProgressHasChanged() throws Exception {
        instantiateStreamPlaya();
        startPlaybackOnSkippy();
        when(skippyAdapter.getProgress()).thenReturn(1L);

        streamPlayerWrapper.onPlaystateChanged(new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_NOT_FOUND, track.getUrn()));
        verify(mediaPlayerAdapter, never()).play(any(Track.class), anyLong());
    }

    @Test
    public void onPlaystateChangedPassesErrorEventToListenerWhenNotAtStartOfPlay() throws Exception {
        instantiateStreamPlaya();
        startPlaybackOnSkippy();
        when(skippyAdapter.getProgress()).thenReturn(1L);

        final Playa.StateTransition stateTransition = new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_NOT_FOUND, track.getUrn());
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
        streamPlayerWrapper.onPlaystateChanged(new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE));
        expect(streamPlayerWrapper.getState()).toBe(Playa.PlayaState.BUFFERING);
    }

    @Test
    public void getLastStateTransitionReturnsIdleNothingByDefault() throws Exception {
        instantiateStreamPlaya();
        final Playa.StateTransition expected = new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.NONE);
        expect(streamPlayerWrapper.getLastStateTransition()).toEqual(expected);
    }

    @Test
    public void getLastStateTransitionReturnsLastTransition() throws Exception {
        instantiateStreamPlaya();
        final Playa.StateTransition stateTransition = new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE);
        streamPlayerWrapper.onPlaystateChanged(stateTransition);
        expect(streamPlayerWrapper.getLastStateTransition()).toEqual(stateTransition);
    }

    @Test
    public void isPlayingReturnsIsPlayingFromLastTransition() throws Exception {
        instantiateStreamPlaya();
        final Playa.StateTransition stateTransition = new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE);
        streamPlayerWrapper.onPlaystateChanged(stateTransition);
        expect(streamPlayerWrapper.isPlaying()).toEqual(true);
    }

    @Test
    public void isPlayerPlayingReturnsIsPlayerPlayingFromLastTransition() throws Exception {
        instantiateStreamPlaya();
        final Playa.StateTransition stateTransition = new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE);
        streamPlayerWrapper.onPlaystateChanged(stateTransition);
        expect(streamPlayerWrapper.isPlaying()).toEqual(true);
    }

    @Test
    public void isBufferingReturnsIsPlayerPlayingFromLastTransition() throws Exception {
        instantiateStreamPlaya();
        final Playa.StateTransition stateTransition = new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE);
        streamPlayerWrapper.onPlaystateChanged(stateTransition);
        expect(streamPlayerWrapper.isPlaying()).toEqual(true);
    }

    @Test
    public void playbackHasPausedReturnTrueIfLastStateTransitionIsIdleNone() throws Exception {
        instantiateStreamPlaya();
        final Playa.StateTransition stateTransition = new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.NONE);
        streamPlayerWrapper.onPlaystateChanged(stateTransition);
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
        when(sharedPreferences.getBoolean(DeveloperPreferences.DEV_FORCE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.play(track);
    }
}
