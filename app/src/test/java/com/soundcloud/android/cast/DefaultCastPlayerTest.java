package com.soundcloud.android.cast;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.soundcloud.android.playback.PlayStateReason;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.ProgressReporter;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueue;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DefaultCastPlayerTest extends AndroidUnitTest {

    private static final Urn TRACK_URN1 = Urn.forTrack(123L);
    private static final Urn TRACK_URN2 = Urn.forTrack(456L);
    private static final Urn TRACK_URN3 = Urn.forTrack(789L);
    private static final List<Urn> PLAY_QUEUE = Arrays.asList(TRACK_URN1, TRACK_URN2);

    private static final PlayQueueItem PLAY_QUEUE_ITEM1 = TestPlayQueueItem.createTrack(TRACK_URN1);

    private static final long fakeProgress = 123L;
    private static final long fakeDuration = 456L;

    private DefaultCastPlayer castPlayer;
    private TestEventBus eventBus = new TestEventBus();
    private TestObserver<PlaybackResult> observer;

    @Mock private DefaultCastOperations castOperations;
    @Mock private ApplicationProperties applicationProperties;
    @Mock private GoogleApiClient googleApiClient;
    @Mock private PendingResult<RemoteMediaPlayer.MediaChannelResult> pendingResultCallback;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private RemoteMediaClient remoteMediaClient;
    @Mock private PlaySessionStateProvider playSessionStateProvider;
    @Mock private CastProtocol castProtocol;
    @Mock private CastQueueController castQueueController;
    @Mock private CastPlayStateReporter castPlayStateReporter;
    @Mock private PlaybackProgress playbackProgress;

    @Captor private ArgumentCaptor<PlaybackStateTransition> transitionArgumentCaptor;
    @Captor private ArgumentCaptor<ProgressReporter.ProgressPuller> progressPusherArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        castPlayer = getCastPlayer();
        castPlayer.onConnected();
        observer = new TestObserver<>();
    }

    private DefaultCastPlayer getCastPlayer() {
        when(castProtocol.getRemoteMediaClient()).thenReturn(remoteMediaClient);

        return new DefaultCastPlayer(castOperations, playQueueManager, eventBus,
                                     castProtocol, playSessionStateProvider,
                                     castQueueController, castPlayStateReporter);
    }

    @Test
    public void credentialsAreAttachedToProtocolOnConnection() {
        // onConnected called in setUp

        verify(castProtocol).attachCredentials(castOperations.getCastCredentials());
    }

    @Test
    public void pushProgressSendsProgressReportToListener() {
        final long progress = 123L;
        final long duration = 456L;
        final Urn urn = TRACK_URN1;
        when(castQueueController.getRemoteCurrentTrackUrn()).thenReturn(urn);

        castPlayer.onProgressUpdated(progress, duration);

        PlaybackProgress playbackProgress = eventBus.lastEventOn(EventQueue.PLAYBACK_PROGRESS).getPlaybackProgress();
        assertThat(playbackProgress.getPosition()).isEqualTo(progress);
        assertThat(playbackProgress.getDuration()).isEqualTo(duration);
        assertThat(playbackProgress.getUrn()).isEqualTo(urn);
    }

    private void mockPlayerState(Urn urn, int playerState, int idleReason, long progress, long duration) {
        when(castQueueController.getRemoteCurrentTrackUrn()).thenReturn(urn);
        when(remoteMediaClient.getPlayerState()).thenReturn(playerState);
        when(remoteMediaClient.getIdleReason()).thenReturn(idleReason);
        mockProgressAndDuration(progress, duration);
    }

    @Test
    public void onStatusUpdatedWithPlayingStateReturnsPlayingNone() {
        mockPlayerState(TRACK_URN1, MediaStatus.PLAYER_STATE_PLAYING, MediaStatus.IDLE_REASON_NONE, fakeProgress, fakeDuration);

        castPlayer.reportPlayerState();

        verify(castPlayStateReporter).reportPlaying(TRACK_URN1, fakeProgress, fakeDuration);
    }

    @Test
    public void onStatusUpdatedWithPausedStateReturnsIdleNone() {
        mockPlayerState(TRACK_URN1, MediaStatus.PLAYER_STATE_PAUSED, MediaStatus.IDLE_REASON_NONE, fakeProgress, fakeDuration);

        castPlayer.reportPlayerState();

        verify(castPlayStateReporter).reportPaused(TRACK_URN1, fakeProgress, fakeDuration);
    }

    @Test
    public void onStatusUpdatedWithBufferingStateReturnsBufferingNone() {
        mockPlayerState(TRACK_URN1, MediaStatus.PLAYER_STATE_BUFFERING, MediaStatus.IDLE_REASON_NONE, fakeProgress, fakeDuration);

        castPlayer.reportPlayerState();

        verify(castPlayStateReporter).reportBuffering(TRACK_URN1, fakeProgress, fakeDuration);
    }

    @Test
    public void onStatusUpdatedWithIdleErrorStateReturnsIdleFailed() {
        mockPlayerState(TRACK_URN1, MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_ERROR, fakeProgress, fakeDuration);

        castPlayer.reportPlayerState();

        verify(castPlayStateReporter).reportIdle(PlayStateReason.ERROR_FAILED, TRACK_URN1, fakeProgress, fakeDuration);
    }

    @Test
    public void onStatusUpdatedWithIdleFinishedStateReturnsTrackComplete() {
        mockPlayerState(TRACK_URN1, MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_FINISHED, fakeProgress, fakeDuration);

        castPlayer.reportPlayerState();

        verify(castPlayStateReporter).reportIdle(PlayStateReason.PLAYBACK_COMPLETE, TRACK_URN1, fakeProgress, fakeDuration);
    }

    @Test
    public void onStatusUpdatedWithIdleCancelledStateReturnsIdleNone() {
        mockPlayerState(TRACK_URN1, MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_CANCELED, fakeProgress, fakeDuration);

        castPlayer.reportPlayerState();

        verify(castPlayStateReporter).reportIdle(PlayStateReason.NONE, TRACK_URN1, fakeProgress, fakeDuration);
    }

    @Test
    public void onStatusUpdatedWithIdleInterruptedStateDoesNotReportTranslatedState() {
        mockPlayerState(TRACK_URN1, MediaStatus.PLAYER_STATE_IDLE, MediaStatus.IDLE_REASON_INTERRUPTED, fakeProgress, fakeDuration);

        castPlayer.reportPlayerState();

        eventBus.verifyNoEventsOn(EventQueue.PLAYBACK_STATE_CHANGED);
    }

    @Test
    public void onStatusUpdatedWithIdleUnknownStateDoesNotReportTranslatedState() {
        mockPlayerState(TRACK_URN1, MediaStatus.PLAYER_STATE_IDLE, MediaStatus.PLAYER_STATE_UNKNOWN, fakeProgress, fakeDuration);

        castPlayer.reportPlayerState();

        eventBus.verifyNoEventsOn(EventQueue.PLAYBACK_STATE_CHANGED);
    }

    @Test
    public void playCurrentLoadsPlayQueueRemotelyWithAutoplayIfRemoteQueueIsEmpty() {
        long playPosition = 123L;
        boolean autoplay = true;
        List<Urn> tracks = Arrays.asList(TRACK_URN1, TRACK_URN2, TRACK_URN3);
        final LoadMessageParameters loadMessageParameters = new LoadMessageParameters(autoplay, playPosition, new JSONObject());
        when(castQueueController.getCurrentQueue()).thenReturn(new CastPlayQueue(Urn.NOT_SET, emptyList()));
        when(playQueueManager.getCurrentQueueTrackUrns()).thenReturn(tracks);
        when(castOperations.createLoadMessageParameters(TRACK_URN1, autoplay, playPosition, tracks))
                .thenReturn(Observable.just(loadMessageParameters));
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(TRACK_URN1));
        mockProgressAndDuration(123L, 124L);

        castPlayer.playCurrent();

        verify(castProtocol).sendLoad(eq(TRACK_URN1.toString()),
                                      eq(autoplay),
                                      eq(playPosition),
                                      eq(loadMessageParameters.jsonData));
    }

    @Test
    public void playCurrentReportsRemotePlayerStateToInternalPlaybackStackIfRemoteQueueIsNotEmptyAndFetchedAnAlreadyPlayingQueue() {
        Urn currentTrack = TRACK_URN1;
        mockProgressAndDuration(fakeProgress, fakeDuration);
        when(castQueueController.getCurrentQueue()).thenReturn(new CastPlayQueue(currentTrack, Arrays.asList(currentTrack, TRACK_URN2, TRACK_URN3)));
        when(castQueueController.getRemoteCurrentTrackUrn()).thenReturn(currentTrack);
        when(remoteMediaClient.getPlayerState()).thenReturn(MediaStatus.PLAYER_STATE_PLAYING);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(currentTrack));
        when(castQueueController.isCurrentlyLoadedOnRemotePlayer(currentTrack)).thenReturn(true);

        castPlayer.playCurrent();

        verify(castPlayStateReporter).reportPlaying(currentTrack, fakeProgress, fakeDuration);
    }

    @Test
    public void playCurrentSendsUpdateQueueMessageForIndexChangingIfThereAreRemoteAndLocalQueuesWithTheSameTracks() {
        CastPlayQueue castPlayQueue = new CastPlayQueue(TRACK_URN1, Arrays.asList(TRACK_URN1, TRACK_URN2, TRACK_URN3));
        when(castQueueController.getCurrentQueue()).thenReturn(castPlayQueue);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(TRACK_URN2));
        when(castQueueController.isCurrentlyLoadedOnRemotePlayer(TRACK_URN2)).thenReturn(false);
        when(castQueueController.buildUpdatedCastPlayQueue(any(Urn.class))).thenReturn(CastPlayQueue.forUpdate(TRACK_URN2, castPlayQueue));

        castPlayer.playCurrent();

        verify(castQueueController).buildUpdatedCastPlayQueue(TRACK_URN2);
        verify(castProtocol).sendUpdateQueue(any(CastPlayQueue.class));
    }

    @Test
    public void playCurrentSendsUpdateQueueMessageWithNewTrackSetIfThereAreRemoteAndLocalQueuesButDifferentTracks() {
        CastPlayQueue castPlayQueue = new CastPlayQueue(TRACK_URN1, Arrays.asList(TRACK_URN1, TRACK_URN2, TRACK_URN3));
        when(castQueueController.getCurrentQueue()).thenReturn(castPlayQueue);
        Urn differentQueueTrackUrn = Urn.forTrack(5487L);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(differentQueueTrackUrn));
        when(castQueueController.isCurrentlyLoadedOnRemotePlayer(TRACK_URN2)).thenReturn(false);
        when(castQueueController.buildCastPlayQueue(any(Urn.class), any())).thenReturn(CastPlayQueue.forUpdate(differentQueueTrackUrn, castPlayQueue));

        castPlayer.playCurrent();

        verify(castQueueController).buildCastPlayQueue(eq(differentQueueTrackUrn), any());
        verify(castProtocol).sendUpdateQueue(any(CastPlayQueue.class));
    }

    @Test
    public void playCurrentReconnectsToCurrentSessionIfTrackAlreadyLoaded() {
        long progress = 123456L;
        long duration = 1265498413L;
        Urn currentTrackUrn = TRACK_URN1;
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(playbackProgress);
        when(playbackProgress.getPosition()).thenReturn(progress);
        when(playbackProgress.getDuration()).thenReturn(duration);

        CastPlayQueue castPlayQueue = new CastPlayQueue(currentTrackUrn, PLAY_QUEUE);
        when(castQueueController.getCurrentQueue()).thenReturn(castPlayQueue);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(currentTrackUrn));
        when(castQueueController.isCurrentlyLoadedOnRemotePlayer(currentTrackUrn)).thenReturn(true);
        when(castQueueController.getRemoteCurrentTrackUrn()).thenReturn(currentTrackUrn);
        when(remoteMediaClient.getPlayerState()).thenReturn(MediaStatus.PLAYER_STATE_PLAYING);
        when(remoteMediaClient.getIdleReason()).thenReturn(MediaStatus.IDLE_REASON_NONE);

        castPlayer.playCurrent();

        verify(castPlayStateReporter).reportPlaying(eq(currentTrackUrn), eq(progress), eq(duration));
    }

    @Test
    public void setNewQueueWithSelectedTracks() {
        final List<Urn> urns = Arrays.asList(TRACK_URN1, TRACK_URN2, TRACK_URN3);

        castPlayer.setNewQueue(urns, TRACK_URN1, PlaySessionSource.EMPTY).subscribe(observer);

        verify(castOperations).setNewPlayQueue(urns, TRACK_URN1, PlaySessionSource.EMPTY);
    }

    @Test
    public void setNewQueueEmitsSuccessfulPlaybackResultWhenInitialTrackIsNotDefined() {
        castPlayer.setNewQueue(singletonList(TRACK_URN1), Urn.NOT_SET, PlaySessionSource.EMPTY).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0).isSuccess()).isTrue();
    }

    @Test
    public void setNewQueueEmitsSuccessfulPlaybackResultWhenInitialTrackIsNotFilteredOut() {
        final List<Urn> urns = Arrays.asList(TRACK_URN1, TRACK_URN2, TRACK_URN3);

        castPlayer.setNewQueue(urns, TRACK_URN1, PlaySessionSource.EMPTY).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0).isSuccess()).isTrue();
    }

    @Test
    public void playCallsReportsErrorStateToEventBusOnUnsuccessfulLoad() {
        Urn currentTrackUrn = TRACK_URN1;
        when(castQueueController.getCurrentQueue()).thenReturn(null);
        mockProgressAndDuration(2345L, 3456789L);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(currentTrackUrn));
        when(castOperations.createLoadMessageParameters(eq(currentTrackUrn), anyBoolean(), anyLong(), anyListOf(Urn.class)))
                .thenReturn(Observable.error(new Throwable("loading error")));

        castPlayer.playCurrent();

        verify(castPlayStateReporter).reportPlayingError(currentTrackUrn);
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
    public void getProgressReturnsGetApproximateStreamPositionFromRemoteMediaPlayerWhenConnected() {
        when(castProtocol.isConnected()).thenReturn(true);
        when(remoteMediaClient.getApproximateStreamPosition()).thenReturn(123L);

        assertThat(castPlayer.getProgress()).isEqualTo(123L);
    }

    @Test
    public void getProgressReturnsLastStateFromProviderWhenNotConnected() {
        when(castProtocol.isConnected()).thenReturn(false);
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(new PlaybackProgress(123L, 79864L, TRACK_URN1));

        assertThat(castPlayer.getProgress()).isEqualTo(123L);
    }

    @Test
    public void onDisconnectedBroadcastsIdleState() {
        long progress = 123465L;
        long duration = 451246345L;
        mockProgressAndDuration(progress, duration);
        when(castQueueController.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN1);

        castPlayer.onDisconnected();

        verify(castPlayStateReporter).reportDisconnection(eq(TRACK_URN1), eq(progress), eq(duration));
    }

    @Test
    public void onQueueStatusMessageReceivedDoesNotSetPlayQueueWithSameTrackList() {
        CastPlayQueue castPlayQueue = new CastPlayQueue(TRACK_URN1, PLAY_QUEUE);
        mockForPlayCurrent();
        when(castQueueController.getCurrentQueue()).thenReturn(castPlayQueue);
        when(castQueueController.buildUpdatedCastPlayQueue(any())).thenReturn(castPlayQueue);
        when(playQueueManager.getCurrentQueueTrackUrns()).thenReturn(PLAY_QUEUE);

        castPlayer.onQueueReceived(castPlayQueue);

        verify(playQueueManager, never()).setNewPlayQueue(any(PlayQueue.class), any(PlaySessionSource.class), anyInt());
    }

    @Test
    public void onQueueStatusMessageReceivedPlaysCurrentTrackWithSameRemoteQueueAndRemoteIsPlaying() {
        CastPlayQueue castPlayQueue = new CastPlayQueue(TRACK_URN1, PLAY_QUEUE);
        mockForPlayCurrent();

        when(playQueueManager.getCurrentQueueTrackUrns()).thenReturn(PLAY_QUEUE);
        when(castQueueController.getCurrentQueue()).thenReturn(castPlayQueue);
        when(remoteMediaClient.getPlayerState()).thenReturn(MediaStatus.PLAYER_STATE_PLAYING);
        when(castQueueController.buildUpdatedCastPlayQueue(any())).thenReturn(castPlayQueue);

        castPlayer.onQueueReceived(castPlayQueue);

        verify(playQueueManager).setPosition(castPlayQueue.getCurrentIndex(), true);
    }

    @Test
    public void onQueueStatusMessageReceivedSetsPlayQueueWithDifferentTrackList() {
        CastPlayQueue castPlayQueue = new CastPlayQueue(TRACK_URN1, PLAY_QUEUE);
        mockForPlayCurrent();

        when(castQueueController.getCurrentQueue()).thenReturn(castPlayQueue);
        when(playQueueManager.getCurrentQueueTrackUrns()).thenReturn(Collections.emptyList());
        PlayQueue playQueue = TestPlayQueue.fromTracks(PlaySessionSource.forCast(), emptyMap());
        when(castQueueController.buildPlayQueue(any(), any())).thenReturn(playQueue);
        when(castQueueController.buildUpdatedCastPlayQueue(any())).thenReturn(castPlayQueue);

        castPlayer.onQueueReceived(castPlayQueue);

        verify(playQueueManager, never()).setPosition(anyInt(), anyBoolean());
        verify(playQueueManager).setNewPlayQueue(eq(playQueue), eq(PlaySessionSource.forCast()), eq(castPlayQueue.getCurrentIndex()));
    }

    @Test
    public void onQueueStatusMessageReceivedShowsPlayer() {
        CastPlayQueue castPlayQueue = new CastPlayQueue(TRACK_URN1, PLAY_QUEUE);
        when(castQueueController.getCurrentQueue()).thenReturn(castPlayQueue);
        when(castQueueController.buildPlayQueue(any(), any())).thenReturn(TestPlayQueue.fromTracks(PlaySessionSource.forCast(), emptyMap()));
        when(castQueueController.buildUpdatedCastPlayQueue(any())).thenReturn(castPlayQueue);
        mockForPlayCurrent();

        castPlayer.onQueueReceived(castPlayQueue);

        assertThat(eventBus.eventsOn(EventQueue.PLAYER_COMMAND).size()).isEqualTo(1);
        assertThat(eventBus.eventsOn(EventQueue.PLAYER_COMMAND).get(0).isShow()).isTrue();
    }

    private void mockForPlayCurrent() {
        when(castProtocol.isConnected()).thenReturn(true);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PLAY_QUEUE_ITEM1);
        when(castQueueController.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN1);
        when(castOperations.createLoadMessageParameters(any(Urn.class), anyBoolean(), anyLong(), anyListOf(Urn.class)))
                .thenReturn(Observable.empty());
    }

    @Test
    public void doesNotShowThePlayerWhenTheFetchedRemotePlayQueueIsEmpty() {
        castPlayer.onRemoteEmptyStateFetched();

        eventBus.verifyNoEventsOn(EventQueue.PLAYER_COMMAND);
    }

    @Test
    public void loadLocalQueueWhenFetchedRemotePlayQueueIsEmpty() {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(TRACK_URN1));
        mockProgressAndDuration(0L, 1234L);
        final LoadMessageParameters loadMessageParameters = new LoadMessageParameters(true, 0L, new JSONObject());
        when(castOperations.createLoadMessageParameters(eq(TRACK_URN1), anyBoolean(), anyLong(), any()))
                .thenReturn(Observable.just(loadMessageParameters));

        castPlayer.onRemoteEmptyStateFetched();

        verify(castProtocol).sendLoad(eq(TRACK_URN1.toString()),
                                      anyBoolean(),
                                      anyLong(),
                                      any(JSONObject.class));
    }

    private void mockProgressAndDuration(long progress, long duration) {
        when(castProtocol.isConnected()).thenReturn(true);
        when(remoteMediaClient.getApproximateStreamPosition()).thenReturn(progress);
        when(remoteMediaClient.getStreamDuration()).thenReturn(duration);
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(playbackProgress);
        when(playSessionStateProvider.getLastProgressForItem(any(Urn.class))).thenReturn(playbackProgress);
        when(playbackProgress.getPosition()).thenReturn(progress);
        when(playbackProgress.getDuration()).thenReturn(duration);
    }
}
