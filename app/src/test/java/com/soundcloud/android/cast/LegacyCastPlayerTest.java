package com.soundcloud.android.cast;

import static com.soundcloud.android.testsupport.PlayQueueAssertions.assertPlayQueueSet;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
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
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlayStateReason;
import com.soundcloud.android.playback.PlaybackItem;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.PlaybackState;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.ProgressReporter;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueue;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.utils.CurrentDateProvider;
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
import java.util.List;

public class LegacyCastPlayerTest extends AndroidUnitTest {

    private static final Urn TRACK_URN1 = Urn.forTrack(123L);
    private static final Urn TRACK_URN2 = Urn.forTrack(456L);
    private static final Urn TRACK_URN3 = Urn.forTrack(789L);

    private static final PlayQueueItem PLAY_QUEUE_ITEM1 = TestPlayQueueItem.createTrack(TRACK_URN1);

    private LegacyCastPlayer castPlayer;

    private TestEventBus eventBus = new TestEventBus();
    private TestObserver<PlaybackResult> observer;

    @Mock private LegacyCastOperations castOperations;
    @Mock private VideoCastManager castManager;
    @Mock private ApplicationProperties applicationProperties;
    @Mock private GoogleApiClient googleApiClient;
    @Mock private ProgressReporter progressReporter;
    @Mock private PendingResult<RemoteMediaPlayer.MediaChannelResult> pendingResultCallback;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private CastPlayStatePublisher playStatePublisher;

    @Captor private ArgumentCaptor<PlaybackStateTransition> transitionArgumentCaptor;
    @Captor private ArgumentCaptor<ProgressReporter.ProgressPuller> progressPusherArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        castPlayer = new LegacyCastPlayer(castOperations, castManager, progressReporter, playQueueManager, eventBus,
                                          playStatePublisher, new CurrentDateProvider());
        observer = new TestObserver<>();
    }

    @Test
    public void pushProgressSendsProgressReportToListener() throws Exception {
        when(castManager.getCurrentMediaPosition()).thenReturn(123L);
        when(castManager.getMediaDuration()).thenReturn(456L);
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN1);

        verify(progressReporter).setProgressPuller(progressPusherArgumentCaptor.capture());
        progressPusherArgumentCaptor.getValue().pullProgress();

        verifyProgress(123L, 456L);
    }

    @Test
    public void onStatusUpdatedWithPlayingStateReturnsPlayingNone() throws Exception {
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN1);

        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_PLAYING, MediaStatus.IDLE_REASON_NONE);

        final PlaybackStateTransition stateTransition = captureLastStateTransition();
        assertThat(stateTransition.getNewState()).isSameAs(PlaybackState.PLAYING);
        assertThat(stateTransition.getReason()).isSameAs(PlayStateReason.NONE);
    }

    @Test
    public void onStatusUpdatedWithPlayingStateStartsProgressReporter() throws Exception {
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN1);

        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_PLAYING, MediaStatus.IDLE_REASON_NONE);

        verify(progressReporter).start();
    }

    @Test
    public void onStatusUpdatedWithPausedStateReturnsIdleNone() throws Exception {
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN1);

        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_PAUSED, MediaStatus.IDLE_REASON_NONE);

        final PlaybackStateTransition stateTransition = captureLastStateTransition();
        assertThat(stateTransition.getNewState()).isSameAs(PlaybackState.IDLE);
        assertThat(stateTransition.getReason()).isSameAs(PlayStateReason.NONE);
    }

    @Test
    public void onStatusUpdatedWithPausedStateStopsProgressReporter() throws Exception {
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN1);

        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_PAUSED, MediaStatus.IDLE_REASON_NONE);

        verify(progressReporter).stop();
    }

    @Test
    public void onStatusUpdatedWithBufferingStateReturnsBufferingNone() throws Exception {
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN1);

        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_BUFFERING, MediaStatus.IDLE_REASON_NONE);

        final PlaybackStateTransition stateTransition = captureLastStateTransition();
        assertThat(stateTransition.getNewState()).isSameAs(PlaybackState.BUFFERING);
        assertThat(stateTransition.getReason()).isSameAs(PlayStateReason.NONE);
    }

    @Test
    public void onStatusUpdatedWithBufferingStateStopsProgressReporter() throws Exception {
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN1);

        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_BUFFERING, MediaStatus.IDLE_REASON_NONE);

        verify(progressReporter).stop();
    }

    @Test
    public void onStatusUpdatedWithIdleErrorStateReturnsIdleFailed() throws Exception {
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN1);

        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_ERROR);

        final PlaybackStateTransition stateTransition = captureLastStateTransition();
        assertThat(stateTransition.getNewState()).isSameAs(PlaybackState.IDLE);
        assertThat(stateTransition.getReason()).isSameAs(PlayStateReason.ERROR_FAILED);
    }

    @Test
    public void onStatusUpdatedWithIdleErrorStateStopsProgressReporter() throws Exception {
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN1);

        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_ERROR);

        verify(progressReporter).stop();
    }

    @Test
    public void onStatusUpdatedWithIdleFinishedStateReturnsTrackComplete() throws Exception {
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN1);

        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_FINISHED);

        final PlaybackStateTransition stateTransition = captureLastStateTransition();
        assertThat(stateTransition.getNewState()).isSameAs(PlaybackState.IDLE);
        assertThat(stateTransition.getReason()).isSameAs(PlayStateReason.PLAYBACK_COMPLETE);
    }

    @Test
    public void onStatusUpdatedWithIdleFinishedStateStopsProgressReporter() throws Exception {
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN1);

        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_FINISHED);

        verify(progressReporter).stop();
    }

    @Test
    public void onStatusUpdatedWithIdleCancelledStateReturnsIdleNone() throws Exception {
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN1);

        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_CANCELED);

        final PlaybackStateTransition stateTransition = captureLastStateTransition();
        assertThat(stateTransition.getNewState()).isSameAs(PlaybackState.IDLE);
        assertThat(stateTransition.getReason()).isSameAs(PlayStateReason.NONE);
    }

    @Test
    public void onStatusUpdatedWithIdleCancelledStateStopsProgressReporter() throws Exception {
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN1);

        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_CANCELED);

        verify(progressReporter).stop();
    }

    @Test
    public void onStatusUpdatedWithIdleInterruptedStateDoesNotReportTranslatedState() throws Exception {
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN1);

        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE,
                                                      MediaStatus.IDLE_REASON_INTERRUPTED);

        eventBus.verifyNoEventsOn(EventQueue.PLAYBACK_STATE_CHANGED);
    }

    @Test
    public void onStatusUpdatedWithIdleUnknownStateDoesNotReportTranslatedState() throws Exception {
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN1);

        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE,
                                                      MediaStatus.IDLE_REASON_INTERRUPTED);

        eventBus.verifyNoEventsOn(EventQueue.PLAYBACK_STATE_CHANGED);
    }

    @Test
    public void playWithUrnAlreadyLoadedDoesNotLoadMedia() throws Exception {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PLAY_QUEUE_ITEM1);
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN1);

        castPlayer.playCurrent();

        verify(castManager, never()).loadMedia(any(MediaInfo.class), anyBoolean(), anyInt());
    }

    @Test
    public void playWithUrnAlreadyLoadedOutputsExistingState() throws Exception {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PLAY_QUEUE_ITEM1);
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN1);
        when(castManager.getPlaybackStatus()).thenReturn(MediaStatus.PLAYER_STATE_PLAYING);
        when(castManager.getIdleReason()).thenReturn(MediaStatus.IDLE_REASON_NONE);

        castPlayer.playCurrent();

        expectLastStateTransitionToBe(PlaybackState.PLAYING, PlayStateReason.NONE, TRACK_URN1);

    }

    @Test
    public void playCurrentLoadsPlayQueueRemotely() throws TransientNetworkDisconnectionException, NoConnectionException {
        ArrayList<Urn> localPlayQueueTracks = newArrayList(TRACK_URN1, TRACK_URN2);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PLAY_QUEUE_ITEM1);
        when(playQueueManager.getCurrentQueueTrackUrns()).thenReturn(localPlayQueueTracks);
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN3);
        LocalPlayQueue localPlayQueue = new LocalPlayQueue(new JSONObject(),
                                                           localPlayQueueTracks,
                                                           createMediaInfo(TRACK_URN1),
                                                           TRACK_URN1);
        when(castOperations.loadLocalPlayQueue(TRACK_URN1, localPlayQueueTracks)).thenReturn(Observable.just(
                localPlayQueue));

        castPlayer.playCurrent();

        verify(castManager).loadMedia(eq(localPlayQueue.mediaInfo),
                                      anyBoolean(),
                                      anyInt(),
                                      eq(localPlayQueue.playQueueTracksJSON));
    }

    @Test
    public void playCurrentLoadsMediaWithAutoPlay() throws TransientNetworkDisconnectionException, NoConnectionException {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PLAY_QUEUE_ITEM1);
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN3);
        when(castOperations.loadLocalPlayQueue(eq(TRACK_URN1), anyListOf(Urn.class))).thenReturn(Observable.just(mock(
                LocalPlayQueue.class)));

        castPlayer.playCurrent();

        verify(castManager).loadMedia(or(isNull(), any(MediaInfo.class)), eq(true), anyInt(), or(isNull(), any(JSONObject.class)));
    }

    @Test
    public void playCurrentLoadsMediaWithZeroedPosition() throws TransientNetworkDisconnectionException, NoConnectionException {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PLAY_QUEUE_ITEM1);
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN3);
        when(castOperations.loadLocalPlayQueue(eq(TRACK_URN1), anyListOf(Urn.class))).thenReturn(Observable.just(mock(
                LocalPlayQueue.class)));

        castPlayer.playCurrent();

        verify(castManager).loadMedia(or(isNull(), any(MediaInfo.class)), anyBoolean(), eq(0), or(isNull(), any(JSONObject.class)));
    }

    @Test
    public void playCurrentLoadsMediaWithNonZeroPosition() throws TransientNetworkDisconnectionException, NoConnectionException {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PLAY_QUEUE_ITEM1);
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN3);
        when(castOperations.loadLocalPlayQueue(eq(TRACK_URN1), anyListOf(Urn.class))).thenReturn(Observable.just(mock(
                LocalPlayQueue.class)));

        castPlayer.playCurrent(123L);

        verify(castManager).loadMedia(or(isNull(), any(MediaInfo.class)), anyBoolean(), eq(123), or(isNull(), any(JSONObject.class)));
    }

    @Test
    public void playCurrentReportsBufferingEvent() throws Exception {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PLAY_QUEUE_ITEM1);
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN3);
        when(castOperations.loadLocalPlayQueue(eq(TRACK_URN1), anyListOf(Urn.class))).thenReturn(Observable.just(mock(
                LocalPlayQueue.class)));

        castPlayer.playCurrent();

        expectLastStateTransitionToBe(PlaybackState.BUFFERING, PlayStateReason.NONE, TRACK_URN1);
    }

    @Test
    public void playCurrentReportsBufferingEventBeforeLoadingFinishes() throws Exception {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PLAY_QUEUE_ITEM1);
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN3);
        when(castOperations.loadLocalPlayQueue(eq(TRACK_URN1),
                                               anyListOf(Urn.class))).thenReturn(Observable.empty());

        castPlayer.playCurrent();

        PlaybackState newState = PlaybackState.BUFFERING;
        PlayStateReason reason = PlayStateReason.NONE;
        Urn trackUrn = TRACK_URN1;
        expectLastStateTransitionToBe(newState, reason, trackUrn);
    }

    @Test
    public void reloadCurrentQueueSetsQueueWithRequestedPosition() throws TransientNetworkDisconnectionException, NoConnectionException {
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN1);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PLAY_QUEUE_ITEM1);
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(PlaySessionSource.EMPTY);
        when(castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(eq(TRACK_URN1),
                                                                                 anyListOf(Urn.class))).thenReturn(
                Observable.just(createLocalPlayQueue()));

        castPlayer.reloadCurrentQueue().subscribe(observer);

        final PlayQueue playQueue = TestPlayQueue.fromUrns(Arrays.asList(TRACK_URN1), PlaySessionSource.EMPTY);
        assertPlayQueueSet(playQueueManager, playQueue, PlaySessionSource.EMPTY, 0);
    }

    @Test
    public void reloadCurrentQueueReportsErrorStateToEventBusOnUnsuccessfulLoad() throws TransientNetworkDisconnectionException, NoConnectionException {
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN1);
        when(castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(any(Urn.class),
                                                                                 anyListOf(Urn.class))).thenReturn(
                Observable.error(new Throwable("loading error")));
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PLAY_QUEUE_ITEM1);

        castPlayer.reloadCurrentQueue().subscribe(observer);

        final PlaybackStateTransition stateTransition = captureLastStateTransition();
        assertThat(stateTransition.getNewState()).isSameAs(PlaybackState.IDLE);
        assertThat(stateTransition.getReason()).isSameAs(PlayStateReason.ERROR_FAILED);
        assertThat(stateTransition.getUrn()).isSameAs(TRACK_URN1);
    }

    @Test
    public void setNewQueueEmitsSuccessfulPlaybackResultWhenInitialTrackIsNotDefined() {
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN1);
        final LocalPlayQueue filteredLocalPlayQueue = new LocalPlayQueue(mock(JSONObject.class),
                                                                         Arrays.asList(TRACK_URN1),
                                                                         createMediaInfo(TRACK_URN1),
                                                                         TRACK_URN1);
        when(castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(any(Urn.class),
                                                                                 anyListOf(Urn.class))).thenReturn(
                Observable.just(filteredLocalPlayQueue));

        castPlayer.setNewQueue(TestPlayQueue.fromUrns(Arrays.asList(TRACK_URN1), PlaySessionSource.EMPTY), Urn.NOT_SET, PlaySessionSource.EMPTY).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0).isSuccess()).isTrue();
    }

    @Test
    public void setNewQueueEmitsSuccessfulPlaybackResultWhenInitialTrackIsNotFilteredOut() {
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN1);
        final LocalPlayQueue filteredLocalPlayQueue = new LocalPlayQueue(mock(JSONObject.class),
                                                                         Arrays.asList(TRACK_URN1),
                                                                         createMediaInfo(TRACK_URN1),
                                                                         TRACK_URN1);
        when(castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(any(Urn.class),
                                                                                 anyListOf(Urn.class))).thenReturn(
                Observable.just(filteredLocalPlayQueue));

        castPlayer.setNewQueue(TestPlayQueue.fromUrns(Arrays.asList(TRACK_URN1), PlaySessionSource.EMPTY), TRACK_URN1, PlaySessionSource.EMPTY).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0).isSuccess()).isTrue();
    }

    @Test
    public void setNewQueueEmitsTrackUnavailablePlaybackResultWhenInitialTrackIsFilteredOut() {
        final LocalPlayQueue filteredLocalPlayQueue = new LocalPlayQueue(mock(JSONObject.class),
                                                                         Arrays.asList(TRACK_URN2),
                                                                         createMediaInfo(TRACK_URN2),
                                                                         TRACK_URN2);
        when(castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(any(Urn.class),
                                                                                 anyListOf(Urn.class))).thenReturn(
                Observable.just(filteredLocalPlayQueue));

        castPlayer.setNewQueue(TestPlayQueue.fromUrns(Arrays.asList(TRACK_URN1, TRACK_URN2), PlaySessionSource.EMPTY), TRACK_URN1, PlaySessionSource.EMPTY)
                  .subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0).isSuccess()).isFalse();
        assertThat(observer.getOnNextEvents()
                           .get(0)
                           .getErrorReason()).isEqualTo(PlaybackResult.ErrorReason.TRACK_UNAVAILABLE_CAST);
    }

    @Test
    public void setNewQueueEmitsTrackUnavailablePlaybackResultWhenLocalQueueIsEmpty() {
        when(castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(any(Urn.class),
                                                                                 anyListOf(Urn.class))).thenReturn(
                Observable.just(LocalPlayQueue.empty()));

        castPlayer.setNewQueue(TestPlayQueue.fromUrns(Arrays.asList(TRACK_URN1), PlaySessionSource.EMPTY), TRACK_URN1, PlaySessionSource.EMPTY).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0).isSuccess()).isFalse();
        assertThat(observer.getOnNextEvents()
                           .get(0)
                           .getErrorReason()).isEqualTo(PlaybackResult.ErrorReason.TRACK_UNAVAILABLE_CAST);
    }

    @Test
    public void playCallsReportsErrorStateToEventBusOnUnsuccessfulLoad() throws Exception {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PLAY_QUEUE_ITEM1);
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN3);
        when(castOperations.loadLocalPlayQueue(eq(TRACK_URN1),
                                               anyListOf(Urn.class))).thenReturn(Observable.error(new Throwable(
                "loading error")));

        castPlayer.playCurrent();

        final PlaybackStateTransition stateTransition = captureLastStateTransition();
        assertThat(stateTransition.getNewState()).isSameAs(PlaybackState.IDLE);
        assertThat(stateTransition.getReason()).isSameAs(PlayStateReason.ERROR_FAILED);
        assertThat(stateTransition.getUrn()).isSameAs(TRACK_URN1);
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
        when(castOperations.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN1);

        castPlayer.onDisconnected();

        final PlaybackStateTransition stateTransition = captureLastStateTransition();
        assertThat(stateTransition.getNewState()).isSameAs(PlaybackState.IDLE);
        assertThat(stateTransition.getReason()).isSameAs(PlayStateReason.CAST_DISCONNECTED);
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

    private PlaybackStateTransition captureLastStateTransition() {
        final ArgumentCaptor<PlaybackStateTransition> captor = ArgumentCaptor.forClass(PlaybackStateTransition.class);
        verify(playStatePublisher, atLeastOnce()).publish(captor.capture(), any(PlaybackItem.class));
        final List<PlaybackStateTransition> values = captor.getAllValues();
        return values.isEmpty() ? null : values.get(values.size() - 1);
    }

    private void verifyProgress(long position, long duration) {
        PlaybackProgress playbackProgress = eventBus.lastEventOn(EventQueue.PLAYBACK_PROGRESS).getPlaybackProgress();
        assertThat(playbackProgress.getPosition()).isEqualTo(position);
        assertThat(playbackProgress.getDuration()).isEqualTo(duration);
    }

    private LocalPlayQueue createLocalPlayQueue() {
        return new LocalPlayQueue(mock(JSONObject.class),
                                  Arrays.asList(TRACK_URN1),
                                  createMediaInfo(TRACK_URN1),
                                  TRACK_URN1);
    }

    private void expectLastStateTransitionToBe(PlaybackState newState, PlayStateReason reason, Urn trackUrn) {
        final PlaybackStateTransition stateTransition = captureLastStateTransition();
        assertThat(stateTransition.getNewState()).isSameAs(newState);
        assertThat(stateTransition.getReason()).isSameAs(reason);
        assertThat(stateTransition.getUrn()).isEqualTo(trackUrn);
    }
}
