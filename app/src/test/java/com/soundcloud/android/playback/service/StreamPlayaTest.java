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
import com.soundcloud.android.playback.service.mediaplayer.MediaPlayerAdapter;
import com.soundcloud.android.playback.service.skippy.SkippyAdapter;
import com.soundcloud.android.preferences.DevSettings;
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
    private BufferingPlaya mBufferingPlaya;
    @Mock
    private Playa.PlayaListener playaListener;
    @Mock
    private Track track;

    @Before
    public void setUp() throws Exception {
        when(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor);
        when(sharedPreferencesEditor.putInt(anyString(), anyInt())).thenReturn(sharedPreferencesEditor);
        when(skippyAdapter.init(context)).thenReturn(true);
    }

    @After
    public void tearDown(){
        StreamPlaya.skippyFailedToInitialize = false;
    }

    private void instantiateStreamPlaya() {
        streamPlayerWrapper = new StreamPlaya(context, sharedPreferences, mediaPlayerAdapter, skippyAdapter, mBufferingPlaya);
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
        when(sharedPreferences.getBoolean(DevSettings.DEV_ENABLE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.play(track);
        verify(skippyAdapter).play(track);
    }

    @Test
    public void playUrlSetsPlayListenerOnSkippyPlayerIfPreferenceSet() throws Exception {
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(DevSettings.DEV_ENABLE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.play(track);
        skippyAdapter.play(track);
        verify(skippyAdapter).setListener(streamPlayerWrapper);
    }

    @Test
    public void playUrlPlaysOnMediaPlayerIfSkippyEnabledButSkippyLoadFailed() throws Exception {
        when(skippyAdapter.init(context)).thenReturn(false);
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(DevSettings.DEV_ENABLE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.play(track);
        verify(mediaPlayerAdapter).play(track);
    }

    @Test
    public void mediaPlayerIsStoppedWhenStartingBufferingMode() throws Exception {
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        streamPlayerWrapper.startBufferingMode();
        verify(mediaPlayerAdapter).stop();
    }

    @Test
    public void mediaPlayerListenerSetToNullWhenStartingBufferingMode() throws Exception {
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        when(sharedPreferences.getBoolean(DevSettings.DEV_ENABLE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.startBufferingMode();
        verify(mediaPlayerAdapter).setListener(null);
    }

    @Test
    public void playUrlSetsListenerToSkippy() throws Exception {
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(DevSettings.DEV_ENABLE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.play(track);
        verify(skippyAdapter).setListener(streamPlayerWrapper);
    }

    @Test
    public void skippyPlayerIsStoppedWhenBufferingModeStarted() throws Exception {
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(DevSettings.DEV_ENABLE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.play(track);
        streamPlayerWrapper.startBufferingMode();
        verify(skippyAdapter).stop();
    }

    @Test
    public void skippyPlayerListenerIsSetToNullWhenBufferingModeStarted() throws Exception {
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(DevSettings.DEV_ENABLE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.play(track);
        streamPlayerWrapper.startBufferingMode();
        verify(skippyAdapter).setListener(null);
    }

    @Test
    public void playUrlUsesMediaPlayerWhenTotalNumberOfPlaysHasNotHitThreshhold() throws Exception {
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(DevSettings.DEV_ENABLE_SKIPPY, false)).thenReturn(false);
        when(sharedPreferences.getInt(StreamPlaya.PLAYS_SINCE_SKIPPY, 0)).thenReturn(StreamPlaya.MAX_PLAYS_OFF_SKIPPY - 1);
        streamPlayerWrapper.play(track);
        verify(mediaPlayerAdapter).play(track);
    }

    @Test
    public void playUrlIncreasesCountWhenTotalNumberOfPlayHasNotHitThreshhold() throws Exception {
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(DevSettings.DEV_ENABLE_SKIPPY, false)).thenReturn(false);
        when(sharedPreferences.getInt(StreamPlaya.PLAYS_SINCE_SKIPPY, 0)).thenReturn(StreamPlaya.MAX_PLAYS_OFF_SKIPPY - 1);
        streamPlayerWrapper.play(track);
        verify(sharedPreferencesEditor).putInt(StreamPlaya.PLAYS_SINCE_SKIPPY, StreamPlaya.MAX_PLAYS_OFF_SKIPPY);
        verify(sharedPreferencesEditor).apply();
    }

    @Test
    public void playUrlUsesSkippyWhenTotalPlaysEqualsThreshold() throws Exception {
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(DevSettings.DEV_ENABLE_SKIPPY, false)).thenReturn(false);
        when(sharedPreferences.getInt(StreamPlaya.PLAYS_SINCE_SKIPPY, 0)).thenReturn(StreamPlaya.MAX_PLAYS_OFF_SKIPPY);
        streamPlayerWrapper.play(track);
        verify(skippyAdapter).play(track);
    }

    @Test
    public void playUrlResetsCountWhenTotalPlaysEqualsThreshold() throws Exception {
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(DevSettings.DEV_ENABLE_SKIPPY, false)).thenReturn(false);
        when(sharedPreferences.getInt(StreamPlaya.PLAYS_SINCE_SKIPPY, 0)).thenReturn(StreamPlaya.MAX_PLAYS_OFF_SKIPPY);
        streamPlayerWrapper.play(track);
        verify(sharedPreferencesEditor).putInt(StreamPlaya.PLAYS_SINCE_SKIPPY, 0);
    }

    @Test
    public void playUrlPlaysOnMediaPlayerTotalPlaysEqualsThresholdButSkippyLoadFailed() throws Exception {
        when(skippyAdapter.init(context)).thenReturn(false);
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(DevSettings.DEV_ENABLE_SKIPPY, false)).thenReturn(false);
        when(sharedPreferences.getInt(StreamPlaya.PLAYS_SINCE_SKIPPY, 0)).thenReturn(StreamPlaya.MAX_PLAYS_OFF_SKIPPY);
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
        when(sharedPreferences.getBoolean(DevSettings.DEV_ENABLE_SKIPPY, false)).thenReturn(false);
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
    public void autoRetriesLastPlayOnMediaPlayerIfSkippyErrorsOnStart() throws Exception {
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(DevSettings.DEV_ENABLE_SKIPPY, false)).thenReturn(false);
        when(sharedPreferences.getInt(StreamPlaya.PLAYS_SINCE_SKIPPY, 0)).thenReturn(StreamPlaya.MAX_PLAYS_OFF_SKIPPY);
        when(skippyAdapter.getProgress()).thenReturn(0L);
        streamPlayerWrapper.play(track);

        streamPlayerWrapper.onPlaystateChanged(new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_NOT_FOUND));
        verify(mediaPlayerAdapter).play(track, 0L);
    }

    @Test
    public void autoRetriesLastPlayOnMediaPlayerIfSkippyErrorsOnResumeTime() throws Exception {
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(DevSettings.DEV_ENABLE_SKIPPY, false)).thenReturn(false);
        when(sharedPreferences.getInt(StreamPlaya.PLAYS_SINCE_SKIPPY, 0)).thenReturn(StreamPlaya.MAX_PLAYS_OFF_SKIPPY);

        streamPlayerWrapper.play(track, 500L);
        when(skippyAdapter.getProgress()).thenReturn(500L);

        streamPlayerWrapper.onPlaystateChanged(new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_NOT_FOUND));
        verify(mediaPlayerAdapter).play(track, 500L);
    }

    @Test
    public void doesNotAutoRetrySkippyErrorIfSkippyModeEnabled() throws Exception {
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(DevSettings.DEV_ENABLE_SKIPPY, false)).thenReturn(true);
        startPlaybackOnSkippy();
        streamPlayerWrapper.onPlaystateChanged(new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_NOT_FOUND));
        verify(mediaPlayerAdapter, never()).play(any(Track.class), anyLong());
    }

    @Test
    public void doesNotAutoRetrySkippyErrorIfProgressHasChanged() throws Exception {
        instantiateStreamPlaya();
        startPlaybackOnSkippy();
        when(skippyAdapter.getProgress()).thenReturn(1L);

        streamPlayerWrapper.onPlaystateChanged(new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_NOT_FOUND));
        verify(mediaPlayerAdapter, never()).play(any(Track.class), anyLong());
    }

    @Test
    public void onPlaystateChangedPassesErrorEventToListenerWhenNotAtStartOfPlay() throws Exception {
        instantiateStreamPlaya();
        startPlaybackOnSkippy();
        when(skippyAdapter.getProgress()).thenReturn(1L);

        final Playa.StateTransition stateTransition = new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_NOT_FOUND);
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

    private void startPlaybackOnSkippy() {
        when(sharedPreferences.getBoolean(DevSettings.DEV_ENABLE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.play(track);
    }
}
