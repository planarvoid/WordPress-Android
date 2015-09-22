package com.soundcloud.android.cast;

import static com.soundcloud.java.collections.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
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
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.Player;
import com.soundcloud.android.playback.ProgressReporter;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;

import java.util.ArrayList;
import java.util.Arrays;

public class CastPlayerTest extends AndroidUnitTest {

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

    @Captor private ArgumentCaptor<Player.StateTransition> transitionArgumentCaptor;
    @Captor private ArgumentCaptor<ProgressReporter.ProgressPuller> progressPusherArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        castPlayer = new CastPlayer(castOperations, castManager, progressReporter, playQueueManager, adsOperations, eventBus);
        observer = new TestObserver<>();
    }

    @Test
    public void pushProgressSendsProgressReportToListener() throws Exception {
        when(castManager.getCurrentMediaPosition()).thenReturn(123L);
        when(castManager.getMediaDuration()).thenReturn(456L);

        verify(progressReporter).setProgressPuller(progressPusherArgumentCaptor.capture());
        progressPusherArgumentCaptor.getValue().pullProgress();

        verifyProgress(123L, 456L);
    }

    @Test
    public void onStatusUpdatedWithPlayingStateReturnsPlayingNone() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_PLAYING, MediaStatus.IDLE_REASON_NONE);

        final Player.StateTransition stateTransition = captureLastStateTransition();
        assertThat(stateTransition.getNewState()).isSameAs(Player.PlayerState.PLAYING);
        assertThat(stateTransition.getReason()).isSameAs(Player.Reason.NONE);
    }

    @Test
    public void onStatusUpdatedWithPlayingStateStartsProgressReporter() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_PLAYING, MediaStatus.IDLE_REASON_NONE);

        verify(progressReporter).start();
    }

    @Test
    public void onStatusUpdatedWithPausedStateReturnsIdleNone() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_PAUSED, MediaStatus.IDLE_REASON_NONE);

        final Player.StateTransition stateTransition = captureLastStateTransition();
        assertThat(stateTransition.getNewState()).isSameAs(Player.PlayerState.IDLE);
        assertThat(stateTransition.getReason()).isSameAs(Player.Reason.NONE);
    }

    @Test
    public void onStatusUpdatedWithPausedStateStopsProgressReporter() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_PAUSED, MediaStatus.IDLE_REASON_NONE);

        verify(progressReporter).stop();
    }

    @Test
    public void onStatusUpdatedWithBufferingStateReturnsBufferingNone() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_BUFFERING, MediaStatus.IDLE_REASON_NONE);

        final Player.StateTransition stateTransition = captureLastStateTransition();
        assertThat(stateTransition.getNewState()).isSameAs(Player.PlayerState.BUFFERING);
        assertThat(stateTransition.getReason()).isSameAs(Player.Reason.NONE);
    }

    @Test
    public void onStatusUpdatedWithBufferingStateStopsProgressReporter() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_BUFFERING, MediaStatus.IDLE_REASON_NONE);

        verify(progressReporter).stop();
    }

    @Test
    public void onStatusUpdatedWithIdleErrorStateReturnsIdleFailed() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_ERROR);

        final Player.StateTransition stateTransition = captureLastStateTransition();
        assertThat(stateTransition.getNewState()).isSameAs(Player.PlayerState.IDLE);
        assertThat(stateTransition.getReason()).isSameAs(Player.Reason.ERROR_FAILED);
    }

    @Test
    public void onStatusUpdatedWithIdleErrorStateStopsProgressReporter() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_ERROR);

        verify(progressReporter).stop();
    }

    @Test
    public void onStatusUpdatedWithIdleFinishedStateReturnsTrackComplete() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_FINISHED);

        final Player.StateTransition stateTransition = captureLastStateTransition();
        assertThat(stateTransition.getNewState()).isSameAs(Player.PlayerState.IDLE);
        assertThat(stateTransition.getReason()).isSameAs(Player.Reason.TRACK_COMPLETE);
    }

    @Test
    public void onStatusUpdatedWithIdleFinishedStateStopsProgressReporter() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_FINISHED);

        verify(progressReporter).stop();
    }

    @Test
    public void onStatusUpdatedWithIdleCancelledStateReturnsIdleNone() throws Exception {
        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_CANCELED);

        final Player.StateTransition stateTransition = captureLastStateTransition();
        assertThat(stateTransition.getNewState()).isSameAs(Player.PlayerState.IDLE);
        assertThat(stateTransition.getReason()).isSameAs(Player.Reason.NONE);
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

        expectLastStateTransitionToBe(Player.PlayerState.PLAYING, Player.Reason.NONE, TRACK_URN1);

    }

    @Test
    public void playCurrentLoadsPlayQueueRemotely() throws TransientNetworkDisconnectionException, NoConnectionException {
        ArrayList<Urn> localPlayQueueTracks = newArrayList(TRACK_URN1, TRACK_URN2);
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
    public void playCurrentLoadsMediaWithNonZeroPosition() throws TransientNetworkDisconnectionException, NoConnectionException {
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(TRACK_URN1);
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN3);
        when(castOperations.loadLocalPlayQueue(eq(TRACK_URN1), anyListOf(Urn.class))).thenReturn(Observable.just(mock(LocalPlayQueue.class)));

        castPlayer.playCurrent(123L);

        verify(castManager).loadMedia(any(MediaInfo.class), anyBoolean(), eq(123), any(JSONObject.class));
    }

    @Test
    public void playCurrentReportsBufferingEvent() throws Exception {
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(TRACK_URN1);
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN3);
        when(castOperations.loadLocalPlayQueue(eq(TRACK_URN1), anyListOf(Urn.class))).thenReturn(Observable.just(mock(LocalPlayQueue.class)));

        castPlayer.playCurrent();

        expectLastStateTransitionToBe(Player.PlayerState.BUFFERING, Player.Reason.NONE, TRACK_URN1);
    }

    @Test
    public void playCurrentReportsBufferingEventBeforeLoadingFinishes() throws Exception {
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(TRACK_URN1);
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN3);
        when(castOperations.loadLocalPlayQueue(eq(TRACK_URN1), anyListOf(Urn.class))).thenReturn(Observable.<LocalPlayQueue>empty());

        castPlayer.playCurrent();

        Player.PlayerState newState = Player.PlayerState.BUFFERING;
        Player.Reason reason = Player.Reason.NONE;
        Urn trackUrn = TRACK_URN1;
        expectLastStateTransitionToBe(newState, reason, trackUrn);
    }

    @Test
    public void reloadCurrentQueueSetsQueueWithRequestedPosition() throws TransientNetworkDisconnectionException, NoConnectionException {
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(TRACK_URN1);
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(PlaySessionSource.EMPTY);
        when(castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(eq(TRACK_URN1), anyListOf(Urn.class))).thenReturn(Observable.just(createLocalPlayQueue()));

        castPlayer.reloadCurrentQueue().subscribe(observer);

        verify(playQueueManager).setNewPlayQueue(PlayQueue.fromTrackUrnList(Arrays.asList(TRACK_URN1), PlaySessionSource.EMPTY), PlaySessionSource.EMPTY, 0);
    }

    @Test
    public void reloadCurrentQueueReportsErrorStateToEventBusOnUnsuccessfulLoad() throws TransientNetworkDisconnectionException, NoConnectionException {
        when(castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(any(Urn.class), anyListOf(Urn.class))).thenReturn(Observable.<LocalPlayQueue>error(new Throwable("loading error")));
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(TRACK_URN1);

        castPlayer.reloadCurrentQueue().subscribe(observer);

        final Player.StateTransition stateTransition = captureLastStateTransition();
        assertThat(stateTransition.getNewState()).isSameAs(Player.PlayerState.IDLE);
        assertThat(stateTransition.getReason()).isSameAs(Player.Reason.ERROR_FAILED);
        assertThat(stateTransition.getTrackUrn()).isSameAs(TRACK_URN1);
    }

    @Test
    public void setNewQueueEmitsSuccessfulPlaybackResultWhenInitialTrackIsNotDefined() {
        final LocalPlayQueue filteredLocalPlayQueue = new LocalPlayQueue(mock(JSONObject.class), Arrays.asList(TRACK_URN1), createMediaInfo(TRACK_URN1), TRACK_URN1);
        when(castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(any(Urn.class), anyListOf(Urn.class))).thenReturn(Observable.just(filteredLocalPlayQueue));

        castPlayer.setNewQueue(Arrays.asList(TRACK_URN1), Urn.NOT_SET, PlaySessionSource.EMPTY).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0).isSuccess()).isTrue();
    }

    @Test
    public void setNewQueueEmitsSuccessfulPlaybackResultWhenInitialTrackIsNotFilteredOut() {
        final LocalPlayQueue filteredLocalPlayQueue = new LocalPlayQueue(mock(JSONObject.class), Arrays.asList(TRACK_URN1), createMediaInfo(TRACK_URN1), TRACK_URN1);
        when(castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(any(Urn.class), anyListOf(Urn.class))).thenReturn(Observable.just(filteredLocalPlayQueue));

        castPlayer.setNewQueue(Arrays.asList(TRACK_URN1), TRACK_URN1, PlaySessionSource.EMPTY).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0).isSuccess()).isTrue();
    }

    @Test
    public void setNewQueueEmitsTrackUnavailablePlaybackResultWhenInitialTrackIsFilteredOut() {
        final LocalPlayQueue filteredLocalPlayQueue = new LocalPlayQueue(mock(JSONObject.class), Arrays.asList(TRACK_URN2), createMediaInfo(TRACK_URN2), TRACK_URN2);
        when(castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(any(Urn.class), anyListOf(Urn.class))).thenReturn(Observable.just(filteredLocalPlayQueue));

        castPlayer.setNewQueue(Arrays.asList(TRACK_URN1, TRACK_URN2), TRACK_URN1, PlaySessionSource.EMPTY).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0).isSuccess()).isFalse();
        assertThat(observer.getOnNextEvents().get(0).getErrorReason()).isEqualTo(PlaybackResult.ErrorReason.TRACK_UNAVAILABLE_CAST);
    }

    @Test
    public void setNewQueueEmitsTrackUnavailablePlaybackResultWhenLocalQueueIsEmpty() {
        when(castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(any(Urn.class), anyListOf(Urn.class))).thenReturn(Observable.just(LocalPlayQueue.empty()));

        castPlayer.setNewQueue(Arrays.asList(TRACK_URN1), TRACK_URN1, PlaySessionSource.EMPTY).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0).isSuccess()).isFalse();
        assertThat(observer.getOnNextEvents().get(0).getErrorReason()).isEqualTo(PlaybackResult.ErrorReason.TRACK_UNAVAILABLE_CAST);
    }

    @Test
    public void playCallsReportsErrorStateToEventBusOnUnsuccessfulLoad() throws Exception {
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(TRACK_URN1);
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN3);
        when(castOperations.loadLocalPlayQueue(eq(TRACK_URN1), anyListOf(Urn.class))).thenReturn(Observable.<LocalPlayQueue>error(new Throwable("loading error")));

        castPlayer.playCurrent();

        final Player.StateTransition stateTransition = captureLastStateTransition();
        assertThat(stateTransition.getNewState()).isSameAs(Player.PlayerState.IDLE);
        assertThat(stateTransition.getReason()).isSameAs(Player.Reason.ERROR_FAILED);
        assertThat(stateTransition.getTrackUrn()).isSameAs(TRACK_URN1);
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

        assertThat(castPlayer.getProgress()).isEqualTo(123L);
    }

    @Test
    public void onDisconnectedBroadcastsIdleState() throws Exception {
        castPlayer.onDisconnected();

        final Player.StateTransition stateTransition = captureLastStateTransition();
        assertThat(stateTransition.getNewState()).isSameAs(Player.PlayerState.IDLE);
        assertThat(stateTransition.getReason()).isSameAs(Player.Reason.NONE);
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

    private Player.StateTransition captureLastStateTransition() {
        return eventBus.lastEventOn(EventQueue.PLAYBACK_STATE_CHANGED);
    }

    private void verifyProgress(long position, long duration) {
        PlaybackProgress playbackProgress = eventBus.lastEventOn(EventQueue.PLAYBACK_PROGRESS).getPlaybackProgress();
        assertThat(playbackProgress.getPosition()).isEqualTo(position);
        assertThat(playbackProgress.getDuration()).isEqualTo(duration);
    }

    private LocalPlayQueue createLocalPlayQueue() {
        return new LocalPlayQueue(mock(JSONObject.class), Arrays.asList(TRACK_URN1), createMediaInfo(TRACK_URN1), TRACK_URN1);
    }

    private void expectLastStateTransitionToBe(Player.PlayerState newState, Player.Reason reason, Urn trackUrn) {
        final Player.StateTransition stateTransition = captureLastStateTransition();
        assertThat(stateTransition.getNewState()).isSameAs(newState);
        assertThat(stateTransition.getReason()).isSameAs(reason);
        assertThat(stateTransition.getTrackUrn()).isEqualTo(trackUrn);
    }
}
