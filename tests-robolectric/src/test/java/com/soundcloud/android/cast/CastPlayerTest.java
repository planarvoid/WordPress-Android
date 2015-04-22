package com.soundcloud.android.cast;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.google.common.collect.Lists;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ProgressReporter;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;

import java.util.ArrayList;

@RunWith(SoundCloudTestRunner.class)
public class CastPlayerTest {

    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private static final Urn TRACK_URN2 = Urn.forTrack(456L);
    private static final Urn TRACK_URN3 = Urn.forTrack(789L);

    private CastPlayer castPlayer;

    private TestEventBus eventBus = new TestEventBus();

    @Mock private CastOperations castOperations;
    @Mock private VideoCastManager castManager;
    @Mock private ApplicationProperties applicationProperties;
    @Mock private GoogleApiClient googleApiClient;
    @Mock private ProgressReporter progressReporter;
    @Mock private PendingResult<RemoteMediaPlayer.MediaChannelResult> pendingResultCallback;
    @Mock private PlayQueueManager playQueueManager;

    @Captor private ArgumentCaptor<Playa.StateTransition> transitionArgumentCaptor;
    @Captor private ArgumentCaptor<ProgressReporter.ProgressPusher> progressPusherArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        castPlayer = new CastPlayer(castOperations, castManager, progressReporter, eventBus, playQueueManager);
    }

    @Test
    public void pushProgressSendsProgressReportToListener() throws Exception {
        when(castManager.getCurrentMediaPosition()).thenReturn(123L);
        when(castManager.getMediaDuration()).thenReturn(456L);

        verify(progressReporter).setProgressPusher(progressPusherArgumentCaptor.capture());
        progressPusherArgumentCaptor.getValue().pushProgress();

        verifyProgress(123L, 456L);
    }

    @Test
    public void onStatusUpdatedWithPlayingStateReturnsPlayingNone() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_PLAYING, MediaStatus.IDLE_REASON_NONE);

        final Playa.StateTransition stateTransition = captureFirstStateTransition();
        expect(stateTransition.getNewState()).toBe(Playa.PlayaState.PLAYING);
        expect(stateTransition.getReason()).toBe(Playa.Reason.NONE);
    }

    @Test
    public void onStatusUpdatedWithPlayingStateStartsProgressReporter() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_PLAYING, MediaStatus.IDLE_REASON_NONE);

        verify(progressReporter).start();
    }

    @Test
    public void onStatusUpdatedWithPausedStateReturnsIdleNone() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_PAUSED, MediaStatus.IDLE_REASON_NONE);

        final Playa.StateTransition stateTransition = captureFirstStateTransition();
        expect(stateTransition.getNewState()).toBe(Playa.PlayaState.IDLE);
        expect(stateTransition.getReason()).toBe(Playa.Reason.NONE);
    }

    @Test
    public void onStatusUpdatedWithPausedStateStopsProgressReporter() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_PAUSED, MediaStatus.IDLE_REASON_NONE);

        verify(progressReporter).stop();
    }

    @Test
    public void onStatusUpdatedWithBufferingStateReturnsBufferingNone() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_BUFFERING, MediaStatus.IDLE_REASON_NONE);

        final Playa.StateTransition stateTransition = captureFirstStateTransition();
        expect(stateTransition.getNewState()).toBe(Playa.PlayaState.BUFFERING);
        expect(stateTransition.getReason()).toBe(Playa.Reason.NONE);
    }

    @Test
    public void onStatusUpdatedWithBufferingStateStopsProgressReporter() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_BUFFERING, MediaStatus.IDLE_REASON_NONE);

        verify(progressReporter).stop();
    }

    @Test
    public void onStatusUpdatedWithIdleErrorStateReturnsIdleFailed() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_ERROR);

        final Playa.StateTransition stateTransition = captureFirstStateTransition();
        expect(stateTransition.getNewState()).toBe(Playa.PlayaState.IDLE);
        expect(stateTransition.getReason()).toBe(Playa.Reason.ERROR_FAILED);
    }

    @Test
    public void onStatusUpdatedWithIdleErrorStateStopsProgressReporter() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_ERROR);

        verify(progressReporter).stop();
    }

    @Test
    public void onStatusUpdatedWithIdleFinishedStateReturnsTrackComplete() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_FINISHED);

        final Playa.StateTransition stateTransition = captureFirstStateTransition();
        expect(stateTransition.getNewState()).toBe(Playa.PlayaState.IDLE);
        expect(stateTransition.getReason()).toBe(Playa.Reason.TRACK_COMPLETE);
    }

    @Test
    public void onStatusUpdatedWithIdleFinishedStateStopsProgressReporter() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_FINISHED);

        verify(progressReporter).stop();
    }

    @Test
    public void onStatusUpdatedWithIdleCancelledStateReturnsIdleNone() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_CANCELED);

        final Playa.StateTransition stateTransition = captureFirstStateTransition();
        expect(stateTransition.getNewState()).toBe(Playa.PlayaState.IDLE);
        expect(stateTransition.getReason()).toBe(Playa.Reason.NONE);
    }

    @Test
    public void onStatusUpdatedWithIdleCancelledStateStopsProgressReporter() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_CANCELED);

        verify(progressReporter).stop();
    }

    @Test
    public void onStatusUpdatedWithIdleInterruptedStateDoesNotReportTranslatedState() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_INTERRUPTED);

        eventBus.verifyNoEventsOn(EventQueue.PLAYBACK_STATE_CHANGED);
    }

    @Test
    public void onStatusUpdatedWithIdleUnknownStateDoesNotReportTranslatedState() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_INTERRUPTED);

        eventBus.verifyNoEventsOn(EventQueue.PLAYBACK_STATE_CHANGED);
    }

    @Test
    public void playWithUrnAlreadyLoadedDoesNotLoadMedia() throws Exception {
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(TRACK_URN);
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN);

        castPlayer.playCurrent();

        verify(castManager, never()).loadMedia(any(MediaInfo.class), anyBoolean(), anyInt());
    }

    @Test
    public void playWithUrnAlreadyLoadedOutputsExistingState() throws Exception {
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(TRACK_URN);
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN);
        when(castManager.getPlaybackStatus()).thenReturn(MediaStatus.PLAYER_STATE_PLAYING);
        when(castManager.getIdleReason()).thenReturn(MediaStatus.IDLE_REASON_NONE);

        castPlayer.playCurrent();

        final Playa.StateTransition stateTransition = captureFirstStateTransition();
        expect(stateTransition.getNewState()).toBe(Playa.PlayaState.PLAYING);
        expect(stateTransition.getReason()).toBe(Playa.Reason.NONE);
        expect(stateTransition.getTrackUrn()).toEqual(TRACK_URN);

    }

    @Test
    public void playCurrentLoadsPlayQueueRemotely() throws TransientNetworkDisconnectionException, NoConnectionException {
        ArrayList<Urn> localPlayQueueTracks = Lists.newArrayList(TRACK_URN, TRACK_URN2);
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(TRACK_URN);
        when(playQueueManager.getCurrentQueueAsUrnList()).thenReturn(localPlayQueueTracks);
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN3);
        LocalPlayQueue localPlayQueue = new LocalPlayQueue(new JSONObject(), localPlayQueueTracks, createMediaInfo(TRACK_URN), TRACK_URN);
        when(castOperations.loadLocalPlayQueue(TRACK_URN, localPlayQueueTracks)).thenReturn(Observable.just(localPlayQueue));

        castPlayer.playCurrent();

        verify(castManager).loadMedia(eq(localPlayQueue.mediaInfo), anyBoolean(), anyInt(), eq(localPlayQueue.playQueueTracksJSON));
    }

    @Test
    public void playCurrentLoadsMediaWithAutoPlay() throws TransientNetworkDisconnectionException, NoConnectionException {
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(TRACK_URN);
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN3);
        when(castOperations.loadLocalPlayQueue(eq(TRACK_URN), anyListOf(Urn.class))).thenReturn(Observable.just(mock(LocalPlayQueue.class)));

        castPlayer.playCurrent();

        verify(castManager).loadMedia(any(MediaInfo.class), eq(true), anyInt(), any(JSONObject.class));
    }

    @Test
    public void playCurrentLoadsMediaWithDefaultPosition() throws TransientNetworkDisconnectionException, NoConnectionException {
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(TRACK_URN);
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN3);
        when(castOperations.loadLocalPlayQueue(eq(TRACK_URN), anyListOf(Urn.class))).thenReturn(Observable.just(mock(LocalPlayQueue.class)));

        castPlayer.playCurrent();

        verify(castManager).loadMedia(any(MediaInfo.class), anyBoolean(), eq(0), any(JSONObject.class));
    }

    @Test
    public void playCurrentLoadsMediaWithRequestedPosition() throws TransientNetworkDisconnectionException, NoConnectionException {
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(TRACK_URN);
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN3);
        when(castOperations.loadLocalPlayQueue(eq(TRACK_URN), anyListOf(Urn.class))).thenReturn(Observable.just(mock(LocalPlayQueue.class)));

        castPlayer.playCurrent(100);

        verify(castManager).loadMedia(any(MediaInfo.class), anyBoolean(), eq(100), any(JSONObject.class));
    }

    @Test
    public void playReportsBufferingEvent() throws Exception {
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(TRACK_URN);
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN3);
        when(castOperations.loadLocalPlayQueue(eq(TRACK_URN), anyListOf(Urn.class))).thenReturn(Observable.just(mock(LocalPlayQueue.class)));

        castPlayer.playCurrent(100);

        final Playa.StateTransition stateTransition = captureFirstStateTransition();
        expect(stateTransition.getNewState()).toBe(Playa.PlayaState.BUFFERING);
        expect(stateTransition.getReason()).toBe(Playa.Reason.NONE);
        expect(stateTransition.getTrackUrn()).toEqual(TRACK_URN);
    }

    @Test
    public void playCallsReportsErrorStateToEventBusOnUnsuccessfulLoad() throws Exception {
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(TRACK_URN);
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN3);
        when(castOperations.loadLocalPlayQueue(eq(TRACK_URN), anyListOf(Urn.class))).thenReturn(Observable.<LocalPlayQueue>error(new Throwable("loading error")));

        castPlayer.playCurrent();

        final Playa.StateTransition stateTransition = captureFirstStateTransition();
        expect(stateTransition.getNewState()).toBe(Playa.PlayaState.IDLE);
        expect(stateTransition.getReason()).toBe(Playa.Reason.ERROR_FAILED);
        expect(stateTransition.getTrackUrn()).toBe(TRACK_URN);
    }

    @Test
    public void resumeCallsResumeOnRemoteMediaPlayerWithApiClient() throws Exception {
        castPlayer.resume();

        verify(castManager).play();
    }

    @Test
    public void pauseCallsPauseOnRemoteMediaPlayerWithApiClient() throws Exception {
        castPlayer.pause();

        verify(castManager).pause();
    }

    @Test
    public void stopCallsPausepOnRemoteMediaPlayer() throws Exception {
        castPlayer.stop();

        verify(castManager).pause();
    }

    @Test
    public void getProgressReturnsGetApproximateStreamPositionFromRemoteMediaPlayer() throws Exception {
        when(castManager.getCurrentMediaPosition()).thenReturn(123L);

        expect(castPlayer.getProgress()).toEqual(123L);
    }

    @Test
    public void onDisconnectedBroadcastsIdleState() throws Exception {
        castPlayer.onDisconnected();

        final Playa.StateTransition stateTransition = captureFirstStateTransition();
        expect(stateTransition.getNewState()).toBe(Playa.PlayaState.IDLE);
        expect(stateTransition.getReason()).toBe(Playa.Reason.NONE);
    }

    private MediaInfo createMediaInfo(Urn urn) {
        MediaMetadata mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);
        mediaMetadata.putString("urn", String.valueOf(urn));

        return new MediaInfo.Builder("some-url")
                .setContentType("audio/mpeg")
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(mediaMetadata)
                .build();
    }

    private Playa.StateTransition captureFirstStateTransition() {
        return eventBus.lastEventOn(EventQueue.PLAYBACK_STATE_CHANGED);
    }

    private void verifyProgress(long position, long duration) {
        PlaybackProgress playbackProgress = eventBus.lastEventOn(EventQueue.PLAYBACK_PROGRESS).getPlaybackProgress();
        expect(playbackProgress.getPosition()).toEqual(position);
        expect(playbackProgress.getDuration()).toEqual(duration);
    }
}