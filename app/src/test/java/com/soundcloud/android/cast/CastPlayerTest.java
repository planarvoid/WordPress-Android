package com.soundcloud.android.cast;

import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static com.soundcloud.android.testsupport.fixtures.TestPlayStates.URN;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlayStatePublisher;
import com.soundcloud.android.playback.PlayStateReason;
import com.soundcloud.android.playback.PlaybackItem;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.PlaybackState;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.ProgressReporter;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.testsupport.AndroidUnitTest;
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
import rx.observers.TestSubscriber;

import javax.inject.Provider;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CastPlayerTest extends AndroidUnitTest {

    private static final Urn TRACK_URN1 = Urn.forTrack(123L);
    private static final Urn TRACK_URN2 = Urn.forTrack(456L);
    private static final Urn TRACK_URN3 = Urn.forTrack(789L);
    private static final List<Urn> PLAY_QUEUE = Arrays.asList(TRACK_URN1, TRACK_URN2);

    private static final PlayQueueItem PLAY_QUEUE_ITEM1 = TestPlayQueueItem.createTrack(TRACK_URN1);

    private CastPlayer castPlayer;
    private TestEventBus eventBus = new TestEventBus();
    private TestObserver<PlaybackResult> observer;
    private TestSubscriber expandPlayerSubscriber = new TestSubscriber();
    private Provider expandPlayerSubscriberProvider = providerOf(expandPlayerSubscriber);

    @Mock private CastOperations castOperations;
    @Mock private ApplicationProperties applicationProperties;
    @Mock private GoogleApiClient googleApiClient;
    @Mock private ProgressReporter progressReporter;
    @Mock private PendingResult<RemoteMediaPlayer.MediaChannelResult> pendingResultCallback;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlayStatePublisher playStatePublisher;
    @Mock private RemoteMediaClient remoteMediaClient;
    @Mock private PlaySessionStateProvider playSessionStateProvider;
    @Mock private CastProtocol castProtocol;

    @Captor private ArgumentCaptor<PlaybackStateTransition> transitionArgumentCaptor;
    @Captor private ArgumentCaptor<ProgressReporter.ProgressPuller> progressPusherArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        castPlayer = getCastPlayer();
        castPlayer.onConnected(remoteMediaClient);
        observer = new TestObserver<>();
    }

    private CastPlayer getCastPlayer() {
        return new CastPlayer(castOperations, progressReporter, playQueueManager, eventBus,
                              playStatePublisher, new CurrentDateProvider(),
                              castProtocol, playSessionStateProvider,
                              expandPlayerSubscriberProvider);
    }

    @Test
    public void pushProgressSendsProgressReportToListener() {
        when(remoteMediaClient.getApproximateStreamPosition()).thenReturn(123L);
        when(remoteMediaClient.getStreamDuration()).thenReturn(456L);
        when(castProtocol.getRemoteCurrentTrackUrn(any(MediaInfo.class))).thenReturn(TRACK_URN1);

        verify(progressReporter).setProgressPuller(progressPusherArgumentCaptor.capture());
        progressPusherArgumentCaptor.getValue().pullProgress();

        verifyProgress(123L, 456L);
    }

    @Test
    public void onStatusUpdatedWithPlayingStateReturnsPlayingNone() {
        when(castProtocol.getRemoteCurrentTrackUrn(any(MediaInfo.class))).thenReturn(TRACK_URN1);

        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_PLAYING, MediaStatus.IDLE_REASON_NONE);

        final PlaybackStateTransition stateTransition = captureLastStateTransition();
        assertThat(stateTransition.getNewState()).isSameAs(PlaybackState.PLAYING);
        assertThat(stateTransition.getReason()).isSameAs(PlayStateReason.NONE);
    }

    @Test
    public void onStatusUpdatedWithPlayingStateStartsProgressReporter() {
        when(castProtocol.getRemoteCurrentTrackUrn(any(MediaInfo.class))).thenReturn(TRACK_URN1);

        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_PLAYING, MediaStatus.IDLE_REASON_NONE);

        verify(progressReporter).start();
    }

    @Test
    public void onStatusUpdatedWithPausedStateReturnsIdleNone() {
        when(castProtocol.getRemoteCurrentTrackUrn(any(MediaInfo.class))).thenReturn(TRACK_URN1);

        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_PAUSED, MediaStatus.IDLE_REASON_NONE);

        final PlaybackStateTransition stateTransition = captureLastStateTransition();
        assertThat(stateTransition.getNewState()).isSameAs(PlaybackState.IDLE);
        assertThat(stateTransition.getReason()).isSameAs(PlayStateReason.NONE);
    }

    @Test
    public void onStatusUpdatedWithPausedStateStopsProgressReporter() {
        when(castProtocol.getRemoteCurrentTrackUrn(any(MediaInfo.class))).thenReturn(TRACK_URN1);

        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_PAUSED, MediaStatus.IDLE_REASON_NONE);

        verify(progressReporter).stop();
    }

    @Test
    public void onStatusUpdatedWithBufferingStateReturnsBufferingNone() {
        when(castProtocol.getRemoteCurrentTrackUrn(any(MediaInfo.class))).thenReturn(TRACK_URN1);

        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_BUFFERING, MediaStatus.IDLE_REASON_NONE);

        final PlaybackStateTransition stateTransition = captureLastStateTransition();
        assertThat(stateTransition.getNewState()).isSameAs(PlaybackState.BUFFERING);
        assertThat(stateTransition.getReason()).isSameAs(PlayStateReason.NONE);
    }

    @Test
    public void onStatusUpdatedWithBufferingStateStopsProgressReporter() {
        when(castProtocol.getRemoteCurrentTrackUrn(any(MediaInfo.class))).thenReturn(TRACK_URN1);

        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_BUFFERING, MediaStatus.IDLE_REASON_NONE);

        verify(progressReporter).stop();
    }

    @Test
    public void onStatusUpdatedWithIdleErrorStateReturnsIdleFailed() {
        when(castProtocol.getRemoteCurrentTrackUrn(any(MediaInfo.class))).thenReturn(TRACK_URN1);

        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_ERROR);

        final PlaybackStateTransition stateTransition = captureLastStateTransition();
        assertThat(stateTransition.getNewState()).isSameAs(PlaybackState.IDLE);
        assertThat(stateTransition.getReason()).isSameAs(PlayStateReason.ERROR_FAILED);
    }

    @Test
    public void onStatusUpdatedWithIdleErrorStateStopsProgressReporter() {
        when(castProtocol.getRemoteCurrentTrackUrn(any(MediaInfo.class))).thenReturn(TRACK_URN1);

        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_ERROR);

        verify(progressReporter).stop();
    }

    @Test
    public void onStatusUpdatedWithIdleFinishedStateReturnsTrackComplete() {
        when(castProtocol.getRemoteCurrentTrackUrn(any(MediaInfo.class))).thenReturn(TRACK_URN1);

        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_FINISHED);

        final PlaybackStateTransition stateTransition = captureLastStateTransition();
        assertThat(stateTransition.getNewState()).isSameAs(PlaybackState.IDLE);
        assertThat(stateTransition.getReason()).isSameAs(PlayStateReason.PLAYBACK_COMPLETE);
    }

    @Test
    public void onStatusUpdatedWithIdleFinishedStateStopsProgressReporter() {
        when(castProtocol.getRemoteCurrentTrackUrn(any(MediaInfo.class))).thenReturn(TRACK_URN1);

        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_FINISHED);

        verify(progressReporter).stop();
    }

    @Test
    public void onStatusUpdatedWithIdleCancelledStateReturnsIdleNone() {
        when(castProtocol.getRemoteCurrentTrackUrn(any(MediaInfo.class))).thenReturn(TRACK_URN1);

        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_CANCELED);

        final PlaybackStateTransition stateTransition = captureLastStateTransition();
        assertThat(stateTransition.getNewState()).isSameAs(PlaybackState.IDLE);
        assertThat(stateTransition.getReason()).isSameAs(PlayStateReason.NONE);
    }

    @Test
    public void onStatusUpdatedWithIdleCancelledStateStopsProgressReporter() {
        when(castProtocol.getRemoteCurrentTrackUrn(any(MediaInfo.class))).thenReturn(TRACK_URN1);

        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_CANCELED);

        verify(progressReporter).stop();
    }

    @Test
    public void onStatusUpdatedWithIdleInterruptedStateDoesNotReportTranslatedState() {
        when(castProtocol.getRemoteCurrentTrackUrn(any(MediaInfo.class))).thenReturn(TRACK_URN1);

        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE,
                                                      MediaStatus.IDLE_REASON_INTERRUPTED);

        eventBus.verifyNoEventsOn(EventQueue.PLAYBACK_STATE_CHANGED);
    }

    @Test
    public void onStatusUpdatedWithIdleUnknownStateDoesNotReportTranslatedState() {
        when(castProtocol.getRemoteCurrentTrackUrn(any(MediaInfo.class))).thenReturn(TRACK_URN1);

        castPlayer.onMediaPlayerStatusUpdatedListener(MediaStatus.PLAYER_STATE_IDLE,
                                                      MediaStatus.IDLE_REASON_INTERRUPTED);

        eventBus.verifyNoEventsOn(EventQueue.PLAYBACK_STATE_CHANGED);
    }

    @Test
    public void playCurrentLoadsPlayQueueRemotely() {
        final List<Urn> localPlayQueueTracks = newArrayList(TRACK_URN1, TRACK_URN2);
        final LocalPlayQueue localPlayQueue =
                new LocalPlayQueue(new JSONObject(), localPlayQueueTracks, createMediaInfo(TRACK_URN1), TRACK_URN1);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PLAY_QUEUE_ITEM1);
        when(playQueueManager.getCurrentQueueTrackUrns()).thenReturn(localPlayQueueTracks);
        when(castProtocol.getRemoteCurrentTrackUrn(any(MediaInfo.class))).thenReturn(TRACK_URN3);

        when(castOperations.loadLocalPlayQueue(TRACK_URN1, localPlayQueueTracks))
                .thenReturn(Observable.just(localPlayQueue));

        castPlayer.playCurrent();

        verify(remoteMediaClient).load(eq(localPlayQueue.mediaInfo),
                                       anyBoolean(),
                                       anyInt(),
                                       eq(localPlayQueue.playQueueTracksJSON));
    }

    @Test
    public void playCurrentLoadsMediaWithAutoPlay() {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PLAY_QUEUE_ITEM1);
        when(castProtocol.getRemoteCurrentTrackUrn(any(MediaInfo.class))).thenReturn(TRACK_URN3);
        when(castOperations.loadLocalPlayQueue(eq(TRACK_URN1), anyListOf(Urn.class)))
                .thenReturn(Observable.just(mock(LocalPlayQueue.class)));

        castPlayer.playCurrent();

        verify(remoteMediaClient).load(any(MediaInfo.class), eq(true), anyInt(), any(JSONObject.class));
    }

    @Test
    public void playCurrentLoadsMediaWithZeroedPosition() {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PLAY_QUEUE_ITEM1);
        when(castProtocol.getRemoteCurrentTrackUrn(any(MediaInfo.class))).thenReturn(TRACK_URN3);
        when(castOperations.loadLocalPlayQueue(eq(TRACK_URN1), anyListOf(Urn.class)))
                .thenReturn(Observable.just(mock(LocalPlayQueue.class)));

        castPlayer.playCurrent();

        verify(remoteMediaClient).load(any(MediaInfo.class), anyBoolean(), eq(0L), any(JSONObject.class));
    }

    @Test
    public void playCurrentLoadsMediaWithNonZeroPosition() {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PLAY_QUEUE_ITEM1);
        when(castProtocol.getRemoteCurrentTrackUrn(any(MediaInfo.class))).thenReturn(TRACK_URN3);
        when(castOperations.loadLocalPlayQueue(eq(TRACK_URN1), anyListOf(Urn.class))).thenReturn(Observable.just(mock(
                LocalPlayQueue.class)));

        castPlayer.playCurrent(123L);

        verify(remoteMediaClient).load(any(MediaInfo.class), anyBoolean(), eq(123L), any(JSONObject.class));
    }

    @Test
    public void playCurrentReportsBufferingEvent() throws Exception {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PLAY_QUEUE_ITEM1);
        when(castProtocol.getRemoteCurrentTrackUrn(any(MediaInfo.class))).thenReturn(TRACK_URN3);
        when(castOperations.loadLocalPlayQueue(eq(TRACK_URN1), anyListOf(Urn.class))).thenReturn(Observable.just(mock(
                LocalPlayQueue.class)));

        castPlayer.playCurrent();

        expectLastStateTransitionToBe(PlaybackState.BUFFERING, PlayStateReason.NONE, TRACK_URN1);
    }

    @Test
    public void playCurrentReportsBufferingEventBeforeLoadingFinishes() {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PLAY_QUEUE_ITEM1);
        when(castProtocol.getRemoteCurrentTrackUrn(any(MediaInfo.class))).thenReturn(TRACK_URN3);
        when(castOperations.loadLocalPlayQueue(eq(TRACK_URN1),
                                               anyListOf(Urn.class))).thenReturn(Observable.<LocalPlayQueue>empty());

        castPlayer.playCurrent();

        expectLastStateTransitionToBe(PlaybackState.BUFFERING, PlayStateReason.NONE, TRACK_URN1);
    }

    @Test
    public void reloadCurrentQueueSetsNewPlayQueue() {
        final LocalPlayQueue localPlayQueue = createLocalPlayQueue();
        when(castProtocol.getRemoteCurrentTrackUrn(any(MediaInfo.class))).thenReturn(TRACK_URN1);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PLAY_QUEUE_ITEM1);
        when(playQueueManager.getCurrentPlaySessionSource()).thenReturn(PlaySessionSource.EMPTY);
        when(castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(eq(TRACK_URN1), anyListOf(Urn.class)))
                .thenReturn(Observable.just(localPlayQueue));

        castPlayer.reloadCurrentQueue().subscribe(observer);

        verify(castOperations).setNewPlayQueue(localPlayQueue, PlaySessionSource.EMPTY);
    }

    @Test
    public void reloadCurrentQueueReportsErrorStateToEventBusOnUnsuccessfulLoad() {
        when(castProtocol.getRemoteCurrentTrackUrn(any(MediaInfo.class))).thenReturn(TRACK_URN1);
        when(castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(any(Urn.class), anyListOf(Urn.class)))
                .thenReturn(Observable.<LocalPlayQueue>error(new Throwable("loading error")));
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PLAY_QUEUE_ITEM1);

        castPlayer.reloadCurrentQueue().subscribe(observer);

        final PlaybackStateTransition stateTransition = captureLastStateTransition();
        assertThat(stateTransition.getNewState()).isSameAs(PlaybackState.IDLE);
        assertThat(stateTransition.getReason()).isSameAs(PlayStateReason.ERROR_FAILED);
        assertThat(stateTransition.getUrn()).isSameAs(TRACK_URN1);
    }

    @Test
    public void setNewQueueEmitsSuccessfulPlaybackResultWhenInitialTrackIsNotDefined() {
        when(castProtocol.getRemoteCurrentTrackUrn(any(MediaInfo.class))).thenReturn(TRACK_URN1);
        final LocalPlayQueue filteredLocalPlayQueue = new LocalPlayQueue(mock(JSONObject.class),
                                                                         singletonList(TRACK_URN1),
                                                                         createMediaInfo(TRACK_URN1),
                                                                         TRACK_URN1);
        when(castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(any(Urn.class), anyListOf(Urn.class)))
                .thenReturn(Observable.just(filteredLocalPlayQueue));

        castPlayer.setNewQueue(singletonList(TRACK_URN1), Urn.NOT_SET, PlaySessionSource.EMPTY).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0).isSuccess()).isTrue();
    }

    @Test
    public void setNewQueueEmitsSuccessfulPlaybackResultWhenInitialTrackIsNotFilteredOut() {
        when(castProtocol.getRemoteCurrentTrackUrn(any(MediaInfo.class))).thenReturn(TRACK_URN1);
        final LocalPlayQueue filteredLocalPlayQueue = new LocalPlayQueue(mock(JSONObject.class),
                                                                         singletonList(TRACK_URN1),
                                                                         createMediaInfo(TRACK_URN1),
                                                                         TRACK_URN1);
        when(castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(any(Urn.class), anyListOf(Urn.class)))
                .thenReturn(Observable.just(filteredLocalPlayQueue));

        castPlayer.setNewQueue(singletonList(TRACK_URN1), TRACK_URN1, PlaySessionSource.EMPTY).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0).isSuccess()).isTrue();
    }

    @Test
    public void setNewQueueEmitsTrackUnavailablePlaybackResultWhenInitialTrackIsFilteredOut() {
        final LocalPlayQueue filteredLocalPlayQueue = new LocalPlayQueue(mock(JSONObject.class),
                                                                         singletonList(TRACK_URN2),
                                                                         createMediaInfo(TRACK_URN2),
                                                                         TRACK_URN2);
        when(castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(any(Urn.class), anyListOf(Urn.class)))
                .thenReturn(Observable.just(filteredLocalPlayQueue));

        castPlayer.setNewQueue(Arrays.asList(TRACK_URN1, TRACK_URN2), TRACK_URN1, PlaySessionSource.EMPTY)
                  .subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0).isSuccess()).isFalse();
        assertThat(observer.getOnNextEvents()
                           .get(0).getErrorReason()).isEqualTo(PlaybackResult.ErrorReason.TRACK_UNAVAILABLE_CAST);
    }

    @Test
    public void setNewQueueEmitsTrackUnavailablePlaybackResultWhenLocalQueueIsEmpty() {
        when(castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(any(Urn.class), anyListOf(Urn.class)))
                .thenReturn(Observable.just(LocalPlayQueue.empty()));

        castPlayer.setNewQueue(singletonList(TRACK_URN1), TRACK_URN1, PlaySessionSource.EMPTY).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0).isSuccess()).isFalse();
        assertThat(observer.getOnNextEvents().get(0)
                           .getErrorReason()).isEqualTo(PlaybackResult.ErrorReason.TRACK_UNAVAILABLE_CAST);
    }

    @Test
    public void playCallsReportsErrorStateToEventBusOnUnsuccessfulLoad() {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PLAY_QUEUE_ITEM1);
        when(castProtocol.getRemoteCurrentTrackUrn(any(MediaInfo.class))).thenReturn(TRACK_URN3);
        when(castOperations.loadLocalPlayQueue(eq(TRACK_URN1), anyListOf(Urn.class)))
                .thenReturn(Observable.<LocalPlayQueue>error(new Throwable("loading error")));

        castPlayer.playCurrent();

        final PlaybackStateTransition stateTransition = captureLastStateTransition();
        assertThat(stateTransition.getNewState()).isSameAs(PlaybackState.IDLE);
        assertThat(stateTransition.getReason()).isSameAs(PlayStateReason.ERROR_FAILED);
        assertThat(stateTransition.getUrn()).isSameAs(TRACK_URN1);
    }

    @Test
    public void resumeCallsResumeOnRemoteMediaPlayerWithApiClient() {
        castPlayer.resume();

        verify(remoteMediaClient).play();
    }

    @Test
    public void pauseCallsPauseOnRemoteMediaPlayerWithApiClient() {
        castPlayer.pause();

        verify(remoteMediaClient).pause();
    }

    @Test
    public void stopCallsPausepOnRemoteMediaPlayer() {
        castPlayer.stop();

        verify(remoteMediaClient).pause();
    }

    @Test
    public void getProgressReturnsGetApproximateStreamPositionFromRemoteMediaPlayer() {
        when(remoteMediaClient.getApproximateStreamPosition()).thenReturn(123L);

        assertThat(castPlayer.getProgress()).isEqualTo(123L);
    }

    @Test
    public void onDisconnectedBroadcastsIdleState() {
        when(castProtocol.getRemoteCurrentTrackUrn(any(MediaInfo.class))).thenReturn(TRACK_URN1);

        castPlayer.onDisconnected();

        final PlaybackStateTransition stateTransition = captureLastStateTransition();
        assertThat(stateTransition.getNewState()).isSameAs(PlaybackState.IDLE);
        assertThat(stateTransition.getReason()).isSameAs(PlayStateReason.NONE);
    }

    @Test
    public void onMetaDataUpdatedDoesNotSetPlayQueueWithSameTrackList() {
        when(castOperations.loadRemotePlayQueue(any(MediaInfo.class)))
                .thenReturn(new RemotePlayQueue(PLAY_QUEUE, TRACK_URN1));
        when(playQueueManager.hasSameTrackList(PLAY_QUEUE)).thenReturn(true);

        castPlayer.onMetadataUpdated();

        verify(playQueueManager, never()).setNewPlayQueue(any(PlayQueue.class), any(PlaySessionSource.class), anyInt());
    }

    @Test
    public void updateLocalPlayQueueAndPlayStatePlaysCurrentTrackWithSameRemoteQueueAndRemoteIsPlaying() {
        final RemotePlayQueue remoteQueue = new RemotePlayQueue(PLAY_QUEUE, TRACK_URN1);
        mockForPlayCurrent();

        when(playQueueManager.hasSameTrackList(PLAY_QUEUE)).thenReturn(true);
        when(castOperations.loadRemotePlayQueue(any(MediaInfo.class)))
                .thenReturn(remoteQueue);
        when(remoteMediaClient.getPlayerState()).thenReturn(MediaStatus.PLAYER_STATE_PLAYING);

        castPlayer.updateLocalPlayQueueAndPlayState();

        verify(playQueueManager).setPosition(remoteQueue.getCurrentPosition(), true);
        verify(playQueueManager, never()).setNewPlayQueue(any(PlayQueue.class), any(PlaySessionSource.class), anyInt());
    }

    @Test
    public void updateLocalPlayQueueAndPlayStateSetsPlayQueueWithDifferentTrackList() {
        final RemotePlayQueue remotePlayQueue = new RemotePlayQueue(PLAY_QUEUE, URN);
        mockForPlayCurrent();

        when(castOperations.loadRemotePlayQueue(any(MediaInfo.class))).thenReturn(remotePlayQueue);
        when(playQueueManager.hasSameTrackList(PLAY_QUEUE)).thenReturn(false);

        castPlayer.updateLocalPlayQueueAndPlayState();

        verify(playQueueManager, never()).setPosition(anyInt(), anyBoolean());
        verify(playQueueManager).setNewPlayQueue(any(PlayQueue.class), eq(PlaySessionSource.EMPTY),
                                                 eq(remotePlayQueue.getCurrentPosition()));
    }

    private void mockForPlayCurrent() {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PLAY_QUEUE_ITEM1);
        when(castProtocol.getRemoteCurrentTrackUrn(any(MediaInfo.class))).thenReturn(TRACK_URN1);
        when(castOperations.loadLocalPlayQueue(any(Urn.class), anyListOf(Urn.class)))
                .thenReturn(Observable.<LocalPlayQueue>empty());
    }

    @Test
    public void updateLocalPlayQueueAndPlayStateShowsPlayer() {
        final RemotePlayQueue remotePlayQueue = new RemotePlayQueue(PLAY_QUEUE, URN);
        when(castOperations.loadRemotePlayQueue(any(MediaInfo.class))).thenReturn(remotePlayQueue);
        mockForPlayCurrent();

        castPlayer.updateLocalPlayQueueAndPlayState();

        assertThat(eventBus.eventsOn(EventQueue.PLAYER_COMMAND).size()).isEqualTo(1);
        assertThat(eventBus.eventsOn(EventQueue.PLAYER_COMMAND).get(0).isShow()).isTrue();
    }

    @Test
    public void onMetadataUpdatesDoesUpdateLocalPlayQueueWhenRemoteQueueIsEmpty() {
        final RemotePlayQueue remotePlayQueue = new RemotePlayQueue(Collections.<Urn>emptyList(), Urn.NOT_SET);
        when(castOperations.loadRemotePlayQueue(any(MediaInfo.class))).thenReturn(remotePlayQueue);
        mockForPlayCurrent();

        castPlayer.onMetadataUpdated();

        verifyZeroInteractions(playQueueManager);
    }

    @Test
    public void onDisconnectedWithoutPreviousOnConnectedDoesNotPublishStateChanges() {
        castPlayer = getCastPlayer();

        castPlayer.onDisconnected();

        verifyZeroInteractions(playStatePublisher);
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
        verify(playStatePublisher, atLeastOnce()).publish(captor.capture(), any(PlaybackItem.class), eq(false));
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
                                  singletonList(TRACK_URN1),
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
