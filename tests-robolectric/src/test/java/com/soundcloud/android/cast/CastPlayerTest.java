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
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.ProgressReporter;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.PlaySessionSource;
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
import rx.observers.TestObserver;

import java.util.ArrayList;
import java.util.Arrays;

@RunWith(SoundCloudTestRunner.class)
public class CastPlayerTest {

    private static final Urn TRACK_URN1 = Urn.forTrack(123L);
    private static final Urn TRACK_URN2 = Urn.forTrack(456L);
    private static final Urn TRACK_URN3 = Urn.forTrack(789L);

    private CastPlayer castPlayer;

    private TestEventBus eventBus = new TestEventBus();
    private TestObserver<PlaybackResult> observer;

    @Mock private CastOperations castOperations;
    @Mock private VideoCastManager castManager;
    @Mock private ApplicationProperties applicationProperties;
    @Mock private GoogleApiClient googleApiClient;
    @Mock private ProgressReporter progressReporter;
    @Mock private PendingResult<RemoteMediaPlayer.MediaChannelResult> pendingResultCallback;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private AdsOperations adsOperations;

    @Captor private ArgumentCaptor<Playa.StateTransition> transitionArgumentCaptor;
    @Captor private ArgumentCaptor<ProgressReporter.ProgressPusher> progressPusherArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        castPlayer = new CastPlayer(castOperations, castManager, progressReporter, playQueueManager, adsOperations, eventBus);
        observer = new TestObserver<>();
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

        final Playa.StateTransition stateTransition = captureLastStateTransition();
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

        final Playa.StateTransition stateTransition = captureLastStateTransition();
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

        final Playa.StateTransition stateTransition = captureLastStateTransition();
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

        final Playa.StateTransition stateTransition = captureLastStateTransition();
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

        final Playa.StateTransition stateTransition = captureLastStateTransition();
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

        final Playa.StateTransition stateTransition = captureLastStateTransition();
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
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(TRACK_URN1);
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN1);

        castPlayer.playCurrent();

        verify(castManager, never()).loadMedia(any(MediaInfo.class), anyBoolean(), anyInt());
    }

    @Test
    public void playWithUrnAlreadyLoadedOutputsExistingState() throws Exception {
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(TRACK_URN1);
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN1);
        when(castManager.getPlaybackStatus()).thenReturn(MediaStatus.PLAYER_STATE_PLAYING);
        when(castManager.getIdleReason()).thenReturn(MediaStatus.IDLE_REASON_NONE);

        castPlayer.playCurrent();

        expectLastStateTransitionToBe(Playa.PlayaState.PLAYING, Playa.Reason.NONE, TRACK_URN1);

    }

    @Test
    public void playCurrentLoadsPlayQueueRemotely() throws TransientNetworkDisconnectionException, NoConnectionException {
        ArrayList<Urn> localPlayQueueTracks = Lists.newArrayList(TRACK_URN1, TRACK_URN2);
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(TRACK_URN1);
        when(playQueueManager.getCurrentQueueAsUrnList()).thenReturn(localPlayQueueTracks);
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN3);
        LocalPlayQueue localPlayQueue = new LocalPlayQueue(new JSONObject(), localPlayQueueTracks, createMediaInfo(TRACK_URN1), TRACK_URN1);
        when(castOperations.loadLocalPlayQueue(TRACK_URN1, localPlayQueueTracks)).thenReturn(Observable.just(localPlayQueue));

        castPlayer.playCurrent();

        verify(castManager).loadMedia(eq(localPlayQueue.mediaInfo), anyBoolean(), anyInt(), eq(localPlayQueue.playQueueTracksJSON));
    }

    @Test
    public void playCurrentLoadsMediaWithAutoPlay() throws TransientNetworkDisconnectionException, NoConnectionException {
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(TRACK_URN1);
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN3);
        when(castOperations.loadLocalPlayQueue(eq(TRACK_URN1), anyListOf(Urn.class))).thenReturn(Observable.just(mock(LocalPlayQueue.class)));

        castPlayer.playCurrent();

        verify(castManager).loadMedia(any(MediaInfo.class), eq(true), anyInt(), any(JSONObject.class));
    }

    @Test
    public void playCurrentLoadsMediaWithZeroedPosition() throws TransientNetworkDisconnectionException, NoConnectionException {
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(TRACK_URN1);
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN3);
        when(castOperations.loadLocalPlayQueue(eq(TRACK_URN1), anyListOf(Urn.class))).thenReturn(Observable.just(mock(LocalPlayQueue.class)));

        castPlayer.playCurrent();

        verify(castManager).loadMedia(any(MediaInfo.class), anyBoolean(), eq(0), any(JSONObject.class));
    }

    @Test
    public void playCurrentReportsBufferingEvent() throws Exception {
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(TRACK_URN1);
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN3);
        when(castOperations.loadLocalPlayQueue(eq(TRACK_URN1), anyListOf(Urn.class))).thenReturn(Observable.just(mock(LocalPlayQueue.class)));

        castPlayer.playCurrent();

        expectLastStateTransitionToBe(Playa.PlayaState.BUFFERING, Playa.Reason.NONE, TRACK_URN1);
    }

    @Test
    public void playCurrentReportsBufferingEventBeforeLoadingFinishes() throws Exception {
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(TRACK_URN1);
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN3);
        when(castOperations.loadLocalPlayQueue(eq(TRACK_URN1), anyListOf(Urn.class))).thenReturn(Observable.<LocalPlayQueue>empty());

        castPlayer.playCurrent();

        Playa.PlayaState newState = Playa.PlayaState.BUFFERING;
        Playa.Reason reason = Playa.Reason.NONE;
        Urn trackUrn = TRACK_URN1;
        expectLastStateTransitionToBe(newState, reason, trackUrn);
    }

    @Test
    public void reloadAndPlayCurrentQueueLoadsMediaWithRequestedPosition() throws TransientNetworkDisconnectionException, NoConnectionException {
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(TRACK_URN1);
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(PlaySessionSource.EMPTY);
        when(castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(eq(TRACK_URN1), anyListOf(Urn.class))).thenReturn(Observable.just(createLocalPlayQueue()));

        castPlayer.reloadAndPlayCurrentQueue(100L).subscribe(observer);

        verify(castManager).loadMedia(any(MediaInfo.class), anyBoolean(), eq(100), any(JSONObject.class));
    }

    @Test
    public void reloadAndPlayCurrentQueueReportsBufferingEvent() throws Exception {
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(TRACK_URN1);
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(PlaySessionSource.EMPTY);
        when(castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(eq(TRACK_URN1), anyListOf(Urn.class))).thenReturn(Observable.just(createLocalPlayQueue()));

        castPlayer.reloadAndPlayCurrentQueue(100L).subscribe(observer);

        expectLastStateTransitionToBe(Playa.PlayaState.BUFFERING, Playa.Reason.NONE, TRACK_URN1);
    }

    @Test
    public void reloadAndPlayCurrentQueueLoadsQueueWithoutMonetizableTracks() throws TransientNetworkDisconnectionException, NoConnectionException {
        final LocalPlayQueue localPlayQueue = createLocalPlayQueue();
        when(castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(eq(TRACK_URN1), anyListOf(Urn.class))).thenReturn(Observable.just(localPlayQueue));
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(TRACK_URN1);
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(PlaySessionSource.EMPTY);

        castPlayer.reloadAndPlayCurrentQueue(100L).subscribe(observer);

        verify(castManager).loadMedia(eq(localPlayQueue.mediaInfo), anyBoolean(), anyInt(), eq(localPlayQueue.playQueueTracksJSON));
    }

    @Test
    public void reloadAndPlayCurrentQueueReportsErrorStateToEventBusOnUnsuccessfulLoad() throws TransientNetworkDisconnectionException, NoConnectionException {
        when(castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(any(Urn.class), anyListOf(Urn.class))).thenReturn(Observable.<LocalPlayQueue>error(new Throwable("loading error")));
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(TRACK_URN1);

        castPlayer.reloadAndPlayCurrentQueue(100L).subscribe(observer);

        final Playa.StateTransition stateTransition = captureLastStateTransition();
        expect(stateTransition.getNewState()).toBe(Playa.PlayaState.IDLE);
        expect(stateTransition.getReason()).toBe(Playa.Reason.ERROR_FAILED);
        expect(stateTransition.getTrackUrn()).toBe(TRACK_URN1);
    }

    @Test
    public void playNewQueueEmitsSuccessfulPlaybackResultWhenInitialTrackIsNotDefined() {
        final LocalPlayQueue filteredLocalPlayQueue = new LocalPlayQueue(mock(JSONObject.class), Arrays.asList(TRACK_URN1), createMediaInfo(TRACK_URN1), TRACK_URN1);
        when(castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(any(Urn.class), anyListOf(Urn.class))).thenReturn(Observable.just(filteredLocalPlayQueue));

        castPlayer.playNewQueue(Arrays.asList(TRACK_URN1), Urn.NOT_SET, 0L, PlaySessionSource.EMPTY).subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0).isSuccess()).toBeTrue();
    }

    @Test
    public void playNewQueueEmitsSuccessfulPlaybackResultWhenInitialTrackIsNotFilteredOut() {
        final LocalPlayQueue filteredLocalPlayQueue = new LocalPlayQueue(mock(JSONObject.class), Arrays.asList(TRACK_URN1), createMediaInfo(TRACK_URN1), TRACK_URN1);
        when(castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(any(Urn.class), anyListOf(Urn.class))).thenReturn(Observable.just(filteredLocalPlayQueue));

        castPlayer.playNewQueue(Arrays.asList(TRACK_URN1), TRACK_URN1, 0L, PlaySessionSource.EMPTY).subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0).isSuccess()).toBeTrue();
    }

    @Test
    public void playNewQueueEmitsTrackUnavailablePlaybackResultWhenInitialTrackIsFilteredOut() {
        final LocalPlayQueue filteredLocalPlayQueue = new LocalPlayQueue(mock(JSONObject.class), Arrays.asList(TRACK_URN2), createMediaInfo(TRACK_URN2), TRACK_URN2);
        when(castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(any(Urn.class), anyListOf(Urn.class))).thenReturn(Observable.just(filteredLocalPlayQueue));

        castPlayer.playNewQueue(Arrays.asList(TRACK_URN1, TRACK_URN2), TRACK_URN1, 0L, PlaySessionSource.EMPTY).subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0).isSuccess()).toBeFalse();
        expect(observer.getOnNextEvents().get(0).getErrorReason()).toEqual(PlaybackResult.ErrorReason.TRACK_UNAVAILABLE_CAST);
    }

    @Test
    public void playNewQueueEmitsTrackUnavailablePlaybackResultWhenLocalQueueIsEmpty() {
        when(castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(any(Urn.class), anyListOf(Urn.class))).thenReturn(Observable.just(LocalPlayQueue.empty()));

        castPlayer.playNewQueue(Arrays.asList(TRACK_URN1), TRACK_URN1, 0L, PlaySessionSource.EMPTY).subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0).isSuccess()).toBeFalse();
        expect(observer.getOnNextEvents().get(0).getErrorReason()).toEqual(PlaybackResult.ErrorReason.TRACK_UNAVAILABLE_CAST);
    }

    @Test
    public void playCallsReportsErrorStateToEventBusOnUnsuccessfulLoad() throws Exception {
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(TRACK_URN1);
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN3);
        when(castOperations.loadLocalPlayQueue(eq(TRACK_URN1), anyListOf(Urn.class))).thenReturn(Observable.<LocalPlayQueue>error(new Throwable("loading error")));

        castPlayer.playCurrent();

        final Playa.StateTransition stateTransition = captureLastStateTransition();
        expect(stateTransition.getNewState()).toBe(Playa.PlayaState.IDLE);
        expect(stateTransition.getReason()).toBe(Playa.Reason.ERROR_FAILED);
        expect(stateTransition.getTrackUrn()).toBe(TRACK_URN1);
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

        final Playa.StateTransition stateTransition = captureLastStateTransition();
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

    private Playa.StateTransition captureLastStateTransition() {
        return eventBus.lastEventOn(EventQueue.PLAYBACK_STATE_CHANGED);
    }

    private void verifyProgress(long position, long duration) {
        PlaybackProgress playbackProgress = eventBus.lastEventOn(EventQueue.PLAYBACK_PROGRESS).getPlaybackProgress();
        expect(playbackProgress.getPosition()).toEqual(position);
        expect(playbackProgress.getDuration()).toEqual(duration);
    }

    private LocalPlayQueue createLocalPlayQueue() {
        return new LocalPlayQueue(mock(JSONObject.class), Arrays.asList(TRACK_URN1), createMediaInfo(TRACK_URN1), TRACK_URN1);
    }

    private void expectLastStateTransitionToBe(Playa.PlayaState newState, Playa.Reason reason, Urn trackUrn) {
        final Playa.StateTransition stateTransition = captureLastStateTransition();
        expect(stateTransition.getNewState()).toBe(newState);
        expect(stateTransition.getReason()).toBe(reason);
        expect(stateTransition.getTrackUrn()).toEqual(trackUrn);
    }
}