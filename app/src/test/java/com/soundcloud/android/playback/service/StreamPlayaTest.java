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

import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.mediaplayer.MediaPlayerAdapter;
import com.soundcloud.android.playback.service.skippy.SkippyAdapter;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.settings.GeneralSettings;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.PropertySet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class StreamPlayaTest {

    private static final int MAX_CONSECUTIVE_SKIPPY_PLAYS = 2;
    private static final int MAX_CONSECUTIVE_MP_PLAYS = 4;

    private StreamPlaya streamPlayerWrapper;
    @Mock private Context context;
    @Mock private SharedPreferences sharedPreferences;
    @Mock private SharedPreferences.Editor sharedPreferencesEditor;
    @Mock private MediaPlayerAdapter mediaPlayerAdapter;
    @Mock private SkippyAdapter skippyAdapter;
    @Mock private BufferingPlaya bufferingPlaya;
    @Mock private Playa.PlayaListener playaListener;
    @Mock private StreamPlaya.PlayerSwitcherInfo playerSwitcherInfo;
    @Mock private FeatureOperations featureOperations;

    private PropertySet track;

    @Before
    public void setUp() throws Exception {
        track = TestPropertySets.expectedTrackForPlayer();

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
        streamPlayerWrapper = new StreamPlaya(context, sharedPreferences, mediaPlayerAdapter, skippyAdapter, bufferingPlaya, playerSwitcherInfo, featureOperations);
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
    public void playCallsPlayOnMediaPlayerByDefault() throws Exception {
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        verify(mediaPlayerAdapter).play(track);
    }

    @Test
    public void playSetsListenerOnMediaPlayerByDefault() throws Exception {
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        verify(mediaPlayerAdapter).setListener(streamPlayerWrapper);
    }

    @Test
    public void playCallsPlayOnSkippyPlayerIfPreferenceSet() throws Exception {
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(GeneralSettings.FORCE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.play(track);
        verify(skippyAdapter).play(track);
    }

    @Test
    public void playCallsPlayOnMediaPlayerOnGingerbreadEvenIfPreferenceSet() throws Exception {
        when(playerSwitcherInfo.shouldForceMediaPlayer()).thenReturn(true);
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(GeneralSettings.FORCE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.play(track);
        verify(mediaPlayerAdapter).play(track);
    }

    @Test
    public void playUninterruptedCallsPlayOnMediaPlayerByDefault() throws Exception {
        instantiateStreamPlaya();
        streamPlayerWrapper.playUninterrupted(track);
        verify(mediaPlayerAdapter).playUninterrupted(track);
    }

    @Test
    public void playUninterruptedCallsPlayOnSkippyPlayerIfPreferenceSet() throws Exception {
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(GeneralSettings.FORCE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.playUninterrupted(track);
        verify(skippyAdapter).playUninterrupted(track);
    }

    @Test
    public void playUninterruptedCallsPlayOnMediaPlayerIfOnGingerbreadEvenIfSkippyPreferenceSet() throws Exception {
        when(playerSwitcherInfo.shouldForceMediaPlayer()).thenReturn(true);
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(GeneralSettings.FORCE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.playUninterrupted(track);
        verify(mediaPlayerAdapter).playUninterrupted(track);
    }

    @Test
    public void playCallsPlayOnSkippyPlayerIfConsecutiveMPPlaysAt0() throws Exception {
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(GeneralSettings.FORCE_SKIPPY, false)).thenReturn(false);
        when(playerSwitcherInfo.getMaxConsecutiveMpPlays()).thenReturn(0);
        streamPlayerWrapper.play(track);
        verify(skippyAdapter).play(track);
    }

    @Test
    public void playSetsPlayListenerOnSkippyPlayerIfPreferenceSet() throws Exception {
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(GeneralSettings.FORCE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.play(track);
        skippyAdapter.play(track);
        verify(skippyAdapter).setListener(streamPlayerWrapper);
    }

    @Test
    public void playPlaysOnMediaPlayerIfSkippyEnabledButSkippyLoadFailed() throws Exception {
        when(skippyAdapter.init(context)).thenReturn(false);
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(GeneralSettings.FORCE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.play(track);
        verify(mediaPlayerAdapter).play(track);
    }

    @Test
    public void playCallsPlayOfflineOnSkippyIfTrackIsAvailableOfflineAndNotMarkedForRemoval() throws Exception {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        instantiateStreamPlaya();
        track.put(TrackProperty.OFFLINE_DOWNLOADED_AT, new Date(1000L));

        streamPlayerWrapper.play(track);

        verify(skippyAdapter).playOffline(track, 0);
    }

    @Test
    public void playCallsPlayOfflineOnSkippyWithResumeTimeIfTrackIsAvailableOfflineAndNotMarkedForRemoval() throws Exception {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        instantiateStreamPlaya();
        track.put(TrackProperty.OFFLINE_DOWNLOADED_AT, new Date(1000L));

        streamPlayerWrapper.play(track, 123);

        verify(skippyAdapter).playOffline(track, 123);
    }

    @Test
    public void playCallsPlayOnMediaPlayerIfTrackAvailableOfflineAndSkippyFailedToInitialize() throws Exception {
        when(skippyAdapter.init(context)).thenReturn(false);
        instantiateStreamPlaya();
        track.put(TrackProperty.OFFLINE_DOWNLOADED_AT, new Date(1000L));

        streamPlayerWrapper.play(track);

        verify(mediaPlayerAdapter).play(track);
    }

    @Test
    public void playCallsPlayOnMediaPlayerIfTrackAvailableOfflineButMarkedForRemoval() throws Exception {
        instantiateStreamPlaya();
        track.put(TrackProperty.OFFLINE_DOWNLOADED_AT, new Date(1000L));
        track.put(TrackProperty.OFFLINE_REMOVED_AT, new Date(2000L));

        streamPlayerWrapper.play(track);

        verify(mediaPlayerAdapter).play(track);
    }

    @Test
    public void playCallsPlayOnSkippyIfSkippyModeForcedAndTrackAvailableOfflineButMarkedForRemoval() throws Exception {
        instantiateStreamPlaya();
        track.put(TrackProperty.OFFLINE_DOWNLOADED_AT, new Date(1000L));
        track.put(TrackProperty.OFFLINE_REMOVED_AT, new Date(2000L));
        when(sharedPreferences.getBoolean(GeneralSettings.FORCE_SKIPPY, false)).thenReturn(true);

        streamPlayerWrapper.play(track);

        verify(skippyAdapter).play(track);
    }

    @Test
    public void mediaPlayerIsStoppedPlayIsCalledAndSkippyIsConfigured() throws Exception {
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        when(sharedPreferences.getBoolean(GeneralSettings.FORCE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.play(track);
        verify(mediaPlayerAdapter).stopForTrackTransition();
    }

    @Test
    public void skippyIsStoppedPlayIsCalledAndMediaPlayerIsConfigured() throws Exception {
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(GeneralSettings.FORCE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.play(track);
        when(sharedPreferences.getBoolean(GeneralSettings.FORCE_SKIPPY, false)).thenReturn(false);
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
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        streamPlayerWrapper.startBufferingMode(Urn.forTrack(1L));
        verify(mediaPlayerAdapter).stopForTrackTransition();
    }

    @Test
    public void mediaPlayerIsStoppedWhenStartingBufferingModeBeforeSwitchingToSkippy() throws Exception {
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        when(sharedPreferences.getBoolean(GeneralSettings.FORCE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.startBufferingMode(Urn.forTrack(1L));
        verify(mediaPlayerAdapter).stopForTrackTransition();
    }

    @Test
    public void mediaPlayerListenerSetToNullWhenStartingBufferingMode() throws Exception {
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        when(sharedPreferences.getBoolean(GeneralSettings.FORCE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.startBufferingMode(Urn.forTrack(1L));
        verify(mediaPlayerAdapter).setListener(null);
    }

    @Test
    public void playSetsListenerToSkippy() throws Exception {
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(GeneralSettings.FORCE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.play(track);
        verify(skippyAdapter).setListener(streamPlayerWrapper);
    }

    @Test
    public void skippyPlayerIsStoppedWhenBufferingModeStarted() throws Exception {
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(GeneralSettings.FORCE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.play(track);
        streamPlayerWrapper.startBufferingMode(Urn.forTrack(1L));
        verify(skippyAdapter).stopForTrackTransition();
    }

    @Test
    public void skippyPlayerListenerIsSetToNullWhenBufferingModeStarted() throws Exception {
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(GeneralSettings.FORCE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.play(track);
        streamPlayerWrapper.startBufferingMode(Urn.forTrack(1L));
        verify(skippyAdapter).setListener(null);
    }

    @Test
    public void playUsesMediaPlayerWhenTotalNumberOfPlaysHasNotHitThreshhold() throws Exception {
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(GeneralSettings.FORCE_SKIPPY, false)).thenReturn(false);
        when(sharedPreferences.getInt(StreamPlaya.PLAYS_ON_CURRENT_PLAYER, 0)).thenReturn(MAX_CONSECUTIVE_SKIPPY_PLAYS - 1);
        streamPlayerWrapper.play(track);
        verify(mediaPlayerAdapter).play(track);
    }

    @Test
    public void playIncreasesCountWhenTotalNumberOfPlayHasNotHitThreshhold() throws Exception {
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(GeneralSettings.FORCE_SKIPPY, false)).thenReturn(false);
        when(sharedPreferences.getInt(StreamPlaya.PLAYS_ON_CURRENT_PLAYER, 0)).thenReturn(MAX_CONSECUTIVE_MP_PLAYS - 1);
        streamPlayerWrapper.play(track);
        streamPlayerWrapper.startBufferingMode(Urn.forTrack(1L));

        reset(sharedPreferencesEditor);
        when(sharedPreferencesEditor.putInt(anyString(), anyInt())).thenReturn(sharedPreferencesEditor);
        when(sharedPreferences.getBoolean(GeneralSettings.FORCE_SKIPPY, false)).thenReturn(false);
        when(sharedPreferences.getInt(StreamPlaya.PLAYS_ON_CURRENT_PLAYER, 0)).thenReturn(MAX_CONSECUTIVE_SKIPPY_PLAYS - 1);

        streamPlayerWrapper.play(track);
        verify(sharedPreferencesEditor).putInt(StreamPlaya.PLAYS_ON_CURRENT_PLAYER, MAX_CONSECUTIVE_SKIPPY_PLAYS);
        verify(sharedPreferencesEditor).apply();
    }

    @Test
    public void playUsesSkippyWhenTotalPlaysEqualsThreshold() throws Exception {
        instantiateStreamPlaya();
        streamPlayerWrapper.play(track);
        streamPlayerWrapper.startBufferingMode(Urn.forTrack(1L));

        when(sharedPreferences.getBoolean(GeneralSettings.FORCE_SKIPPY, false)).thenReturn(false);
        when(sharedPreferences.getInt(StreamPlaya.PLAYS_ON_CURRENT_PLAYER, 0)).thenReturn(MAX_CONSECUTIVE_MP_PLAYS);
        streamPlayerWrapper.play(track);
        verify(skippyAdapter).play(track);
    }

    @Test
    public void playResetsCountWhenTotalPlaysEqualsThreshold() throws Exception {
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(GeneralSettings.FORCE_SKIPPY, false)).thenReturn(false);
        when(sharedPreferences.getInt(StreamPlaya.PLAYS_ON_CURRENT_PLAYER, 0)).thenReturn(MAX_CONSECUTIVE_MP_PLAYS);
        streamPlayerWrapper.play(track);
        streamPlayerWrapper.startBufferingMode(Urn.forTrack(1L));

        reset(sharedPreferencesEditor);
        when(sharedPreferencesEditor.putInt(anyString(), anyInt())).thenReturn(sharedPreferencesEditor);
        when(sharedPreferences.getBoolean(GeneralSettings.FORCE_SKIPPY, false)).thenReturn(false);
        when(sharedPreferences.getInt(StreamPlaya.PLAYS_ON_CURRENT_PLAYER, 0)).thenReturn(MAX_CONSECUTIVE_MP_PLAYS);

        streamPlayerWrapper.play(track);
        verify(sharedPreferencesEditor).putInt(StreamPlaya.PLAYS_ON_CURRENT_PLAYER, 1);
    }

    @Test
    public void playPlaysOnMediaPlayerTotalPlaysEqualsThresholdButSkippyLoadFailed() throws Exception {
        when(skippyAdapter.init(context)).thenReturn(false);
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(GeneralSettings.FORCE_SKIPPY, false)).thenReturn(false);
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
        when(sharedPreferences.getBoolean(GeneralSettings.FORCE_SKIPPY, false)).thenReturn(false);
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
    public void autoRetriesLastPlayOnMediaPlayerIfSkippyErrors() throws Exception {
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(GeneralSettings.FORCE_SKIPPY, false)).thenReturn(false);
        when(sharedPreferences.getInt(StreamPlaya.PLAYS_ON_CURRENT_PLAYER, 0)).thenReturn(MAX_CONSECUTIVE_MP_PLAYS);
        when(skippyAdapter.getProgress()).thenReturn(123L);
        streamPlayerWrapper.play(track);

        streamPlayerWrapper.onPlaystateChanged(new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_NOT_FOUND, track.get(TrackProperty.URN)));
        verify(mediaPlayerAdapter).play(track, 123L);
    }

    @Test
    public void doesNotAutoRetrySkippyErrorIfSkippyModeEnabled() throws Exception {
        instantiateStreamPlaya();
        when(sharedPreferences.getBoolean(GeneralSettings.FORCE_SKIPPY, false)).thenReturn(true);
        startPlaybackOnSkippy();
        streamPlayerWrapper.onPlaystateChanged(new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_NOT_FOUND, track.get(TrackProperty.URN)));
        verify(mediaPlayerAdapter, never()).play(any(PropertySet.class), anyLong());
    }

    @Test
    public void doesNotAutoRetrySkippyErrorIfProgressHasChanged() throws Exception {
        instantiateStreamPlaya();
        startPlaybackOnSkippy();
        when(skippyAdapter.getProgress()).thenReturn(1L);

        streamPlayerWrapper.onPlaystateChanged(new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_NOT_FOUND, Urn.forTrack(123L)));
        verify(mediaPlayerAdapter, never()).play(any(PropertySet.class), anyLong());
    }

    @Test
    public void onPlaystateChangedPassesErrorEventToListenerWhenNotAtStartOfPlay() throws Exception {
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
        when(sharedPreferences.getBoolean(GeneralSettings.FORCE_SKIPPY, false)).thenReturn(true);
        streamPlayerWrapper.play(track);
    }
}
