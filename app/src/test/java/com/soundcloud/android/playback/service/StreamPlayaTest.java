package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.mediaplayer.MediaPlayerAdapter;
import com.soundcloud.android.playback.service.skippy.SkippyAdapter;
import com.soundcloud.android.preferences.DevSettings;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.SharedPreferences;

@RunWith(SoundCloudTestRunner.class)
public class StreamPlayaTest {

    private StreamPlaya streamPlayerWrapper;
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
        streamPlayerWrapper = new StreamPlaya(sharedPreferences, mediaPlayerAdapter, skippyAdapter, mBufferingPlaya);
        streamPlayerWrapper.setListener(playaListener);
    }

    @Test
    public void playUrlCallsPlayUrlOnMediaPlayerByDefault() throws Exception {
        streamPlayerWrapper.play(track);
        verify(mediaPlayerAdapter).play(track);
    }

    @Test
    public void playUrlSetsListenerOnMediaPlayerByDefault() throws Exception {
        streamPlayerWrapper.play(track);
        verify(mediaPlayerAdapter).setListener(streamPlayerWrapper);
    }

    @Test
    public void playUrlCallsPlayUrlOnSkippyPlayerIfPreferenceSet() throws Exception {
        when(sharedPreferences.getBoolean(DevSettings.DEV_ENABLE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.play(track);
        verify(skippyAdapter).play(track);
    }

    @Test
    public void playUrlSetsPlayListenerOnSkippyPlayerIfPreferenceSet() throws Exception {
        when(sharedPreferences.getBoolean(DevSettings.DEV_ENABLE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.play(track);
        skippyAdapter.play(track);
        verify(skippyAdapter).setListener(streamPlayerWrapper);
    }

    @Test
    public void mediaPlayerIsStoppedWhenStartingBufferingMode() throws Exception {
        streamPlayerWrapper.play(track);
        streamPlayerWrapper.startBufferingMode();
        verify(mediaPlayerAdapter).stop();
    }

    @Test
    public void mediaPlayerListenerSetToNullWhenStartingBufferingMode() throws Exception {
        streamPlayerWrapper.play(track);
        when(sharedPreferences.getBoolean(DevSettings.DEV_ENABLE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.startBufferingMode();
        verify(mediaPlayerAdapter).setListener(null);
    }

    @Test
    public void playUrlSetsListenerToSkippy() throws Exception {
        when(sharedPreferences.getBoolean(DevSettings.DEV_ENABLE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.play(track);
        verify(skippyAdapter).setListener(streamPlayerWrapper);
    }

    @Test
    public void skippyPlayerIsStoppedWhenBufferingModeStarted() throws Exception {
        when(sharedPreferences.getBoolean(DevSettings.DEV_ENABLE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.play(track);
        streamPlayerWrapper.startBufferingMode();
        verify(skippyAdapter).stop();
    }

    @Test
    public void skippyPlayerListenerIsSetToNullWhenBufferingModeStarted() throws Exception {
        when(sharedPreferences.getBoolean(DevSettings.DEV_ENABLE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.play(track);
        streamPlayerWrapper.startBufferingMode();
        verify(skippyAdapter).setListener(null);
    }

    @Test
    public void playUrlUsesMediaPlayerWhenTotalNumberOfPlaysHasNotHitThreshhold() throws Exception {
        when(sharedPreferences.getBoolean(DevSettings.DEV_ENABLE_SKIPPY, false)).thenReturn(false);
        when(sharedPreferences.getInt(StreamPlaya.PLAYS_SINCE_SKIPPY, 0)).thenReturn(StreamPlaya.MAX_PLAYS_OFF_SKIPPY - 1);
        streamPlayerWrapper.play(track);
        verify(mediaPlayerAdapter).play(track);
    }

    @Test
    public void playUrlIncreasesCountWhenTotalNumberOfPlayHasNotHitThreshhold() throws Exception {
        when(sharedPreferences.getBoolean(DevSettings.DEV_ENABLE_SKIPPY, false)).thenReturn(false);
        when(sharedPreferences.getInt(StreamPlaya.PLAYS_SINCE_SKIPPY, 0)).thenReturn(StreamPlaya.MAX_PLAYS_OFF_SKIPPY - 1);
        streamPlayerWrapper.play(track);
        verify(sharedPreferencesEditor).putInt(StreamPlaya.PLAYS_SINCE_SKIPPY, StreamPlaya.MAX_PLAYS_OFF_SKIPPY);
        verify(sharedPreferencesEditor).apply();
    }

    @Test
    public void playUrlUsesSkippyWhenTotalPlaysEqualsThreshold() throws Exception {
        when(sharedPreferences.getBoolean(DevSettings.DEV_ENABLE_SKIPPY, false)).thenReturn(false);
        when(sharedPreferences.getInt(StreamPlaya.PLAYS_SINCE_SKIPPY, 0)).thenReturn(StreamPlaya.MAX_PLAYS_OFF_SKIPPY);
        streamPlayerWrapper.play(track);
        verify(skippyAdapter).play(track);
    }

    @Test
    public void playUrlResetsCountWhenTotalPlaysEqualsThreshold() throws Exception {
        when(sharedPreferences.getBoolean(DevSettings.DEV_ENABLE_SKIPPY, false)).thenReturn(false);
        when(sharedPreferences.getInt(StreamPlaya.PLAYS_SINCE_SKIPPY, 0)).thenReturn(StreamPlaya.MAX_PLAYS_OFF_SKIPPY);
        streamPlayerWrapper.play(track);
        verify(sharedPreferencesEditor).putInt(StreamPlaya.PLAYS_SINCE_SKIPPY, 0);
    }

    @Test
    public void resumeCallsResumeOnMediaPlayer() throws Exception {
        startPlaybackOnMediaPlayer();
        streamPlayerWrapper.resume();
        verify(mediaPlayerAdapter).resume();
    }

    @Test
    public void pauseCallsPauseOnMediaPlayer() throws Exception {
        startPlaybackOnMediaPlayer();
        streamPlayerWrapper.pause();
        verify(mediaPlayerAdapter).pause();
    }

    @Test
    public void seekCallsSeekOnMediaPlayer() throws Exception {
        startPlaybackOnMediaPlayer();
        streamPlayerWrapper.seek(100, true);
        verify(mediaPlayerAdapter).seek(100, true);
    }

    @Test
    public void getProgressCallsGetProgressOnMediaPlayer() throws Exception {
        startPlaybackOnMediaPlayer();
        streamPlayerWrapper.getProgress();
        verify(mediaPlayerAdapter).getProgress();
    }

    @Test
    public void setVolumeCallsSetVolumeOnMediaPlayer() throws Exception {
        startPlaybackOnMediaPlayer();
        streamPlayerWrapper.setVolume(3.0f);
        verify(mediaPlayerAdapter).setVolume(3.0f);
    }

    @Test
    public void stopCallsStopOnMediaPlayer() throws Exception {
        startPlaybackOnMediaPlayer();
        streamPlayerWrapper.stop();
        verify(mediaPlayerAdapter).stop();
    }

    @Test
    public void getStateReturnsGetStateFromMediaPlayer() throws Exception {
        startPlaybackOnMediaPlayer();
        when(mediaPlayerAdapter.getState()).thenReturn(Playa.PlayaState.BUFFERING);
        expect(streamPlayerWrapper.getState()).toBe(Playa.PlayaState.BUFFERING);
    }

    @Test
    public void getLastReasonReturnsGetLastReasonFromMediaPlayer() throws Exception {
        startPlaybackOnMediaPlayer();
        final Playa.StateTransition stateTransition = new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.ERROR_FAILED);
        when(mediaPlayerAdapter.getLastStateTransition()).thenReturn(stateTransition);
        expect(streamPlayerWrapper.getLastStateTransition()).toBe(stateTransition);
    }

    @Test
    public void isSeekableReturnsMediaPlayerIsSeekable() throws Exception {
        startPlaybackOnMediaPlayer();
        when(mediaPlayerAdapter.isSeekable()).thenReturn(true);
        expect(streamPlayerWrapper.isSeekable()).toBeTrue();
    }

    private void startPlaybackOnMediaPlayer() {
        when(sharedPreferences.getBoolean(DevSettings.DEV_ENABLE_SKIPPY, false)).thenReturn(false);
        streamPlayerWrapper.play(track);
    }

    @Test
    public void isNotSeekablePastBufferReturnsMediaPlayerIsNotSeekablePastBuffer() throws Exception {
        startPlaybackOnMediaPlayer();
        when(mediaPlayerAdapter.isNotSeekablePastBuffer()).thenReturn(true);
        expect(streamPlayerWrapper.isNotSeekablePastBuffer()).toBeTrue();
    }

    @Test
    public void resumeCallsResumeOnSkippyPlayer() throws Exception {
        startPlaybackOnSkippy();
        streamPlayerWrapper.resume();
        verify(skippyAdapter).resume();
    }

    @Test
    public void pauseCallsPauseOnSkippy() throws Exception {
        startPlaybackOnSkippy();
        streamPlayerWrapper.pause();
        verify(skippyAdapter).pause();
    }

    @Test
    public void seekCallsSeekOnSkippy() throws Exception {
        startPlaybackOnSkippy();
        streamPlayerWrapper.seek(100, true);
        verify(skippyAdapter).seek(100, true);
    }

    @Test
    public void getProgressCallsGetProgressOnSkippy() throws Exception {
        startPlaybackOnSkippy();
        streamPlayerWrapper.getProgress();
        verify(skippyAdapter).getProgress();
    }

    @Test
    public void setVolumeCallsSetVolumeOnSkippy() throws Exception {
        startPlaybackOnSkippy();
        streamPlayerWrapper.setVolume(3.0f);
        verify(skippyAdapter).setVolume(3.0f);
    }

    @Test
    public void stopCallsStopOnSkippy() throws Exception {
        startPlaybackOnSkippy();
        streamPlayerWrapper.stop();
        verify(skippyAdapter).stop();
    }

    @Test
    public void getStateReturnsGetStateFromSkippy() throws Exception {
        startPlaybackOnSkippy();
        when(skippyAdapter.getState()).thenReturn(Playa.PlayaState.BUFFERING);
        expect(streamPlayerWrapper.getState()).toBe(Playa.PlayaState.BUFFERING);
    }

    @Test
    public void getLastReasonReturnsGetLastReasonFromSkippy() throws Exception {
        startPlaybackOnSkippy();
        final Playa.StateTransition stateTransition = new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.ERROR_FAILED);
        when(skippyAdapter.getLastStateTransition()).thenReturn(stateTransition);
        expect(streamPlayerWrapper.getLastStateTransition()).toBe(stateTransition);
    }

    @Test
    public void isSeekableReturnsSkippyIsSeekable() throws Exception {
        startPlaybackOnSkippy();
        when(skippyAdapter.isSeekable()).thenReturn(true);
        expect(streamPlayerWrapper.isSeekable()).toBeTrue();
    }

    @Test
    public void isNotSeekablePastBufferReturnsSkippyIsNotSeekablePastBuffer() throws Exception {
        startPlaybackOnSkippy();
        when(skippyAdapter.isNotSeekablePastBuffer()).thenReturn(true);
        expect(streamPlayerWrapper.isNotSeekablePastBuffer()).toBeTrue();
    }

    @Test
    public void destroyCallsDestroyOnBothPlayers() throws Exception {
        streamPlayerWrapper.destroy();
        verify(mediaPlayerAdapter).destroy();
        verify(skippyAdapter).destroy();
    }

    @Test(expected = NullPointerException.class)
    public void onPlaystateChangeDoesThrowsNPEWithSuccessStateAndNoListener(){
        streamPlayerWrapper.setListener(null);
        final Playa.StateTransition transition = new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE);
        streamPlayerWrapper.onPlaystateChanged(transition);
    }

    @Test
    public void onPlaystateChangedPassesSuccessEventToListener() throws Exception {
        streamPlayerWrapper.setListener(playaListener);

        final Playa.StateTransition transition = new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE);
        streamPlayerWrapper.onPlaystateChanged(transition);
        verify(playaListener).onPlaystateChanged(transition);
    }

    @Test
    public void autoRetriesLastPlayOnMediaPlayerIfSkippyErrorsOnStart() throws Exception {
        startPlaybackOnSkippy();
        when(skippyAdapter.getProgress()).thenReturn(0L);

        streamPlayerWrapper.onPlaystateChanged(new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_NOT_FOUND));
        verify(mediaPlayerAdapter).play(track, 0L);
    }

    @Test
    public void autoRetriesLastPlayOnMediaPlayerIfSkippyErrorsOnResumeTime() throws Exception {
        when(sharedPreferences.getBoolean(DevSettings.DEV_ENABLE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.play(track, 500L);
        when(skippyAdapter.getProgress()).thenReturn(500L);

        streamPlayerWrapper.onPlaystateChanged(new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_NOT_FOUND));
        verify(mediaPlayerAdapter).play(track, 500L);
    }

    @Test
    public void doesNotAutoRetrySkippyErrorIfProgressHasChanged() throws Exception {
        startPlaybackOnSkippy();
        when(skippyAdapter.getProgress()).thenReturn(1L);

        streamPlayerWrapper.onPlaystateChanged(new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_NOT_FOUND));
        verify(mediaPlayerAdapter, never()).play(any(Track.class), anyLong());
    }

    @Test
    public void onPlaystateChangedPassesErrorEventToListenerWhenNotAtStartOfPlay() throws Exception {
        startPlaybackOnSkippy();
        when(skippyAdapter.getProgress()).thenReturn(1L);

        final Playa.StateTransition stateTransition = new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_NOT_FOUND);
        streamPlayerWrapper.onPlaystateChanged(stateTransition);
        verify(playaListener).onPlaystateChanged(stateTransition);
    }

    @Test
    public void requestsFocusOnListener() throws Exception {
        streamPlayerWrapper.requestAudioFocus();
        verify(playaListener).requestAudioFocus();
    }

    @Test(expected = NullPointerException.class)
    public void requestsFocusThrowsNPEWithNoListener() throws Exception {
        streamPlayerWrapper.setListener(null);
        streamPlayerWrapper.requestAudioFocus();
    }

    private void startPlaybackOnSkippy() {
        when(sharedPreferences.getBoolean(DevSettings.DEV_ENABLE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.play(track);
    }
}
