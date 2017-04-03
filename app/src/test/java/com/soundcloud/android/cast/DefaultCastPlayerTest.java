package com.soundcloud.android.cast;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.cast.api.CastPlayQueue;
import com.soundcloud.android.cast.api.CastProtocol;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlayStateReason;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueue;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.functions.Action2;
import rx.observers.TestObserver;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DefaultCastPlayerTest extends AndroidUnitTest {

    private static final Urn TRACK_URN1 = Urn.forTrack(123L);
    private static final Urn TRACK_URN2 = Urn.forTrack(456L);
    private static final Urn TRACK_URN3 = Urn.forTrack(789L);
    private static final List<Urn> PLAY_QUEUE = Arrays.asList(TRACK_URN1, TRACK_URN2);

    private static final CastPlayQueue CAST_PLAY_QUEUE = CastPlayQueue.create(TRACK_URN1, PLAY_QUEUE);
    private static final PlayQueueItem PLAY_QUEUE_ITEM1 = TestPlayQueueItem.createTrack(TRACK_URN1);

    private static final long fakeProgress = 123L;
    private static final long fakeDuration = 456L;

    private DefaultCastPlayer castPlayer;
    private TestEventBus eventBus = new TestEventBus();
    private TestObserver<PlaybackResult> observer;

    @Mock private PlayQueueManager playQueueManager;
    @Mock private PlaySessionStateProvider playSessionStateProvider;
    @Mock private CastProtocol castProtocol;
    @Mock private CastQueueController castQueueController;
    @Mock private CastPlayStateReporter castPlayStateReporter;
    @Mock private PlaybackProgress playbackProgress;
    @Mock private CastQueueSlicer castQueueSlicer;

    @Mock private PlayQueue playQueue;

    @Captor private ArgumentCaptor<PlaybackProgressEvent> playbackProgressEventArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        castPlayer = getCastPlayer();
        observer = new TestObserver<>();
    }

    private DefaultCastPlayer getCastPlayer() {
        when(castQueueSlicer.slice(anyList(), anyInt())).thenReturn(playQueue);

        return new DefaultCastPlayer(playQueueManager, eventBus,
                                     castProtocol, playSessionStateProvider,
                                     castQueueController, castPlayStateReporter,
                                     castQueueSlicer);
    }

    @Test
    public void pushProgressUpdatesPlaySessionProvider() {
        final long progress = 123L;
        final long duration = 456L;
        final Urn urn = TRACK_URN1;
        when(castQueueController.getRemoteCurrentTrackUrn()).thenReturn(urn);

        castPlayer.onProgressUpdated(progress, duration);

        verify(playSessionStateProvider).onProgressEvent(playbackProgressEventArgumentCaptor.capture());
        PlaybackProgress playbackProgress = playbackProgressEventArgumentCaptor.getValue().getPlaybackProgress();
        assertThat(playbackProgress.getPosition()).isEqualTo(progress);
        assertThat(playbackProgress.getDuration()).isEqualTo(duration);
        assertThat(playbackProgress.getUrn()).isEqualTo(urn);
    }

    @Test
    public void localPlaybackStateIsUsedAsAutoplayValueWhenLoadingTheQueueOnAnEmptyReceiver() {
        final boolean localPlaybackPlayingState = true;
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(TRACK_URN1));
        when(castQueueController.buildCastPlayQueue(any(Urn.class), any())).thenReturn(CAST_PLAY_QUEUE);
        mockProgressAndDuration(0L, 1234L);

        castPlayer.onConnected(localPlaybackPlayingState);

        castPlayer.onRemoteEmptyStateFetched();
        verify(castProtocol).sendLoad(anyString(), eq(localPlaybackPlayingState), anyLong(), any());
    }

    @Test
    public void onStatusUpdatedWithPlayingStateReturnsPlayingNone() {
        when(castQueueController.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN1);
        mockProgressAndDuration(fakeProgress, fakeDuration);

        castPlayer.onPlaying(fakeProgress, fakeDuration);

        verify(castPlayStateReporter).reportPlaying(TRACK_URN1, fakeProgress, fakeDuration);
    }

    @Test
    public void onStatusUpdatedWithPausedStateReturnsIdleNone() {
        when(castQueueController.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN1);
        mockProgressAndDuration(fakeProgress, fakeDuration);

        castPlayer.onPaused(fakeProgress, fakeDuration);

        verify(castPlayStateReporter).reportPaused(TRACK_URN1, fakeProgress, fakeDuration);
    }

    @Test
    public void onStatusUpdatedWithBufferingStateReturnsBufferingNone() {
        when(castQueueController.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN1);
        mockProgressAndDuration(fakeProgress, fakeDuration);

        castPlayer.onBuffering(fakeProgress, fakeDuration);

        verify(castPlayStateReporter).reportBuffering(TRACK_URN1, fakeProgress, fakeDuration);
    }

    @Test
    public void onStatusUpdatedWithIdleErrorStateReturnsIdleFailed() {
        when(castQueueController.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN1);
        mockProgressAndDuration(fakeProgress, fakeDuration);

        castPlayer.onIdle(fakeProgress, fakeDuration, PlayStateReason.ERROR_FAILED);

        verify(castPlayStateReporter).reportIdle(PlayStateReason.ERROR_FAILED, TRACK_URN1, fakeProgress, fakeDuration);
    }

    @Test
    public void onStatusUpdatedWithIdleAndPlaybackCompletedReasonReportsTheCorrectState() {
        when(castQueueController.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN1);
        mockProgressAndDuration(fakeProgress, fakeDuration);

        castPlayer.onIdle(fakeProgress, fakeDuration, PlayStateReason.PLAYBACK_COMPLETE);

        verify(castPlayStateReporter).reportIdle(PlayStateReason.PLAYBACK_COMPLETE, TRACK_URN1, fakeProgress, fakeDuration);
    }

    @Test
    public void onStatusUpdatedWithIdleWithNoReasonReportsIt() {
        when(castQueueController.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN1);
        mockProgressAndDuration(fakeProgress, fakeDuration);

        castPlayer.onIdle(fakeProgress, fakeDuration, PlayStateReason.NONE);

        verify(castPlayStateReporter).reportIdle(PlayStateReason.NONE, TRACK_URN1, fakeProgress, fakeDuration);
    }

    @Test
    public void playCurrentLoadsCurrentlyPlayingPlayQueueRemotelyWithAutoplayIfRemoteQueueIsEmpty() {
        long playPosition = 123L;
        boolean autoplay = true;
        List<Urn> tracks = Arrays.asList(TRACK_URN1, TRACK_URN2, TRACK_URN3);
        when(castQueueController.getCurrentQueue()).thenReturn(CastPlayQueue.create(Urn.NOT_SET, emptyList()));
        when(playQueueManager.getCurrentQueueTrackUrns()).thenReturn(tracks);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(TRACK_URN1));
        when(castQueueController.buildCastPlayQueue(any(Urn.class), any())).thenReturn(CAST_PLAY_QUEUE);
        when(playSessionStateProvider.isPlaying()).thenReturn(autoplay);
        mockProgressAndDuration(123L, 124L);

        castPlayer.playCurrent();

        verify(castProtocol).sendLoad(eq(TRACK_URN1.toString()),
                                      eq(autoplay),
                                      eq(playPosition),
                                      any(CastPlayQueue.class));
    }

    @Test
    public void playCurrentRequestsSessionStatusIfTrackAlreadyLoaded() {
        final Urn currentTrackUrn = TRACK_URN1;
        when(castQueueController.getCurrentQueue()).thenReturn(CastPlayQueue.create(currentTrackUrn, PLAY_QUEUE));
        when(castQueueController.getRemoteCurrentTrackUrn()).thenReturn(currentTrackUrn);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(currentTrackUrn));
        when(castQueueController.isCurrentlyLoadedOnRemotePlayer(currentTrackUrn)).thenReturn(true);

        castPlayer.playCurrent();

        verify(castProtocol).requestStatusUpdate();
    }

    @Test
    public void playCurrentSendsUpdateQueueMessageForIndexChangingIfThereAreRemoteAndLocalQueuesWithTheSameTracks() {
        CastPlayQueue castPlayQueue = CastPlayQueue.create(TRACK_URN1, Arrays.asList(TRACK_URN1, TRACK_URN2, TRACK_URN3));
        when(castQueueController.getCurrentQueue()).thenReturn(castPlayQueue);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(TRACK_URN2));
        when(castQueueController.isCurrentlyLoadedOnRemotePlayer(TRACK_URN2)).thenReturn(false);
        when(castQueueController.buildUpdatedCastPlayQueue(any(Urn.class), anyLong())).thenReturn(CastPlayQueue.forUpdate(TRACK_URN2, 0L, castPlayQueue));

        castPlayer.playCurrent();

        verify(castQueueController).buildUpdatedCastPlayQueue(eq(TRACK_URN2), anyLong());
        verify(castProtocol).sendUpdateQueue(any(CastPlayQueue.class));
    }

    @Test
    public void playCurrentSendsUpdateQueueMessageWithNewTrackSetIfThereAreRemoteAndLocalQueuesButDifferentTracks() {
        CastPlayQueue castPlayQueue = CastPlayQueue.create(TRACK_URN1, Arrays.asList(TRACK_URN1, TRACK_URN2, TRACK_URN3));
        when(castQueueController.getCurrentQueue()).thenReturn(castPlayQueue);
        Urn differentQueueTrackUrn = Urn.forTrack(5487L);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(differentQueueTrackUrn));
        when(castQueueController.isCurrentlyLoadedOnRemotePlayer(TRACK_URN2)).thenReturn(false);
        when(castQueueController.buildCastPlayQueue(any(Urn.class), any())).thenReturn(CastPlayQueue.forUpdate(differentQueueTrackUrn, 0L, castPlayQueue));

        castPlayer.playCurrent();

        verify(castQueueController).buildCastPlayQueue(eq(differentQueueTrackUrn), any());
        verify(castProtocol).sendUpdateQueue(any(CastPlayQueue.class));
    }

    @Test
    public void playCurrentResumesPlaybackIfTotallyNewQueueIsLoadedOnTheReceiver() {
        when(castQueueController.getCurrentQueue()).thenReturn(CastPlayQueue.create(TRACK_URN1, Arrays.asList(TRACK_URN1, TRACK_URN2, TRACK_URN3)));
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(894561L)));

        castPlayer.playCurrent();

        verify(castProtocol).play();
    }

    @Test
    public void setNewQueueWithSelectedTracks() {
        final List<Urn> urns = Arrays.asList(TRACK_URN1, TRACK_URN2, TRACK_URN3);
        ArgumentCaptor<PlayQueue> playQueueCaptor = ArgumentCaptor.forClass(PlayQueue.class);
        PlayQueue playQueue = TestPlayQueue.fromUrns(urns, PlaySessionSource.EMPTY);
        when(castQueueSlicer.slice(eq(urns), anyInt())).thenReturn(playQueue);

        castPlayer.setNewQueue(playQueue, TRACK_URN1, PlaySessionSource.EMPTY).subscribe(observer);

        verify(playQueueManager).setNewPlayQueue(playQueueCaptor.capture(), eq(PlaySessionSource.EMPTY), anyInt());
        assertThat(playQueueCaptor.getValue().getTrackItemUrns()).isEqualTo(urns);
    }

    @Test
    public void setNewQueueEmitsSuccessfulPlaybackResultWhenInitialTrackIsNotDefined() {
        castPlayer.setNewQueue(TestPlayQueue.fromUrns(singletonList(TRACK_URN1), PlaySessionSource.EMPTY), Urn.NOT_SET, PlaySessionSource.EMPTY).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0).isSuccess()).isTrue();
    }

    @Test
    public void setNewQueueEmitsSuccessfulPlaybackResultWhenInitialTrackIsNotFilteredOut() {
        final List<Urn> urns = Arrays.asList(TRACK_URN1, TRACK_URN2, TRACK_URN3);

        castPlayer.setNewQueue(TestPlayQueue.fromUrns(urns, PlaySessionSource.EMPTY), TRACK_URN1, PlaySessionSource.EMPTY).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0).isSuccess()).isTrue();
    }

    @Test
    public void setNewQueueSlicesTheQueueBeforeUpdatingPlayQueueManager() {
        final List<Urn> urns = Arrays.asList(TRACK_URN1, TRACK_URN2, TRACK_URN3);
        ArgumentCaptor<PlayQueue> playQueueCaptor = ArgumentCaptor.forClass(PlayQueue.class);
        when(castQueueSlicer.slice(eq(urns), anyInt())).thenReturn(playQueue);

        castPlayer.setNewQueue(TestPlayQueue.fromUrns(urns, PlaySessionSource.EMPTY), TRACK_URN1, PlaySessionSource.EMPTY).subscribe(observer);

        verify(castQueueSlicer).slice(urns, 0);
        verify(playQueueManager).setNewPlayQueue(playQueueCaptor.capture(), eq(PlaySessionSource.EMPTY), anyInt());
        assertThat(playQueueCaptor.getValue()).isEqualTo(playQueue);
    }

    @Test
    public void resumeCallsResumeOnRemoteMediaPlayerWithApiClient() {
        castPlayer.resume();

        verify(castProtocol).play();
    }

    @Test
    public void pauseCallsPauseOnRemoteMediaPlayerWithApiClient() {
        castPlayer.pause();

        verify(castProtocol).pause();
    }

    @Test
    public void onDisconnectedBroadcastsIdleStateUsingPreviouslySavedProgressAndDuration() {
        long progress = 123465L;
        long duration = 451246345L;
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(playbackProgress);
        when(playSessionStateProvider.getLastProgressForItem(any(Urn.class))).thenReturn(playbackProgress);
        when(playbackProgress.getPosition()).thenReturn(progress);
        when(playbackProgress.getDuration()).thenReturn(duration);
        when(playbackProgress.getUrn()).thenReturn(TRACK_URN1);
        when(castQueueController.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN1);

        castPlayer.onDisconnected();

        verify(castPlayStateReporter).reportDisconnection(eq(TRACK_URN1), eq(progress), eq(duration));
    }

    @Test
    public void onQueueStatusMessageReceivedDoesNotSetPlayQueueWithSameTrackList() {
        CastPlayQueue castPlayQueue = CastPlayQueue.create(TRACK_URN1, PLAY_QUEUE);
        mockForPlayCurrent();
        when(castQueueController.getCurrentQueue()).thenReturn(castPlayQueue);
        when(castQueueController.buildUpdatedCastPlayQueue(any(), anyLong())).thenReturn(castPlayQueue);
        when(playQueueManager.getCurrentQueueTrackUrns()).thenReturn(PLAY_QUEUE);

        castPlayer.onQueueReceived(castPlayQueue);

        verify(playQueueManager, never()).setNewPlayQueue(any(PlayQueue.class), any(PlaySessionSource.class), anyInt());
    }

    @Test
    public void onQueueStatusMessageReceivedPlaysCurrentTrackWithSameRemoteQueueAndRemoteIsPlaying() {
        CastPlayQueue castPlayQueue = CastPlayQueue.create(TRACK_URN1, PLAY_QUEUE);
        mockForPlayCurrent();

        when(playQueueManager.getCurrentQueueTrackUrns()).thenReturn(PLAY_QUEUE);
        when(castQueueController.getCurrentQueue()).thenReturn(castPlayQueue);
        when(castQueueController.buildUpdatedCastPlayQueue(any(), anyLong())).thenReturn(castPlayQueue);

        castPlayer.onQueueReceived(castPlayQueue);

        verify(playQueueManager).setPosition(castPlayQueue.currentIndex(), true);
    }

    @Test
    public void onQueueStatusMessageReceivedSetsPlayQueueWithDifferentTrackList() {
        CastPlayQueue castPlayQueue = CastPlayQueue.create(TRACK_URN1, PLAY_QUEUE);
        mockForPlayCurrent();

        when(castQueueController.getCurrentQueue()).thenReturn(castPlayQueue);
        when(playQueueManager.getCurrentQueueTrackUrns()).thenReturn(Collections.emptyList());
        PlayQueue playQueue = TestPlayQueue.fromTracks(PlaySessionSource.forCast(), emptyMap());
        when(castQueueController.buildPlayQueue(any(), any())).thenReturn(playQueue);
        when(castQueueController.buildUpdatedCastPlayQueue(any(), anyLong())).thenReturn(castPlayQueue);

        castPlayer.onQueueReceived(castPlayQueue);

        verify(playQueueManager, never()).setPosition(anyInt(), anyBoolean());
        verify(playQueueManager).setNewPlayQueue(eq(playQueue), eq(PlaySessionSource.forCast()), eq(castPlayQueue.currentIndex()));
    }

    @Test
    public void onQueueStatusMessageReceivedShowsPlayer() {
        CastPlayQueue castPlayQueue = CastPlayQueue.create(TRACK_URN1, PLAY_QUEUE);
        when(castQueueController.getCurrentQueue()).thenReturn(castPlayQueue);
        when(castQueueController.buildPlayQueue(any(), any())).thenReturn(TestPlayQueue.fromTracks(PlaySessionSource.forCast(), emptyMap()));
        when(castQueueController.buildUpdatedCastPlayQueue(any(), anyLong())).thenReturn(castPlayQueue);
        mockForPlayCurrent();

        castPlayer.onQueueReceived(castPlayQueue);

        assertThat(eventBus.eventsOn(EventQueue.PLAYER_COMMAND).size()).isEqualTo(1);
        assertThat(eventBus.eventsOn(EventQueue.PLAYER_COMMAND).get(0).isShow()).isTrue();
    }

    private void mockForPlayCurrent() {
        when(castProtocol.isConnected()).thenReturn(true);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PLAY_QUEUE_ITEM1);
        when(castQueueController.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN1);
    }

    @Test
    public void doesNotShowThePlayerWhenTheFetchedRemotePlayQueueIsEmpty() {
        castPlayer.onRemoteEmptyStateFetched();

        eventBus.verifyNoEventsOn(EventQueue.PLAYER_COMMAND);
    }

    @Test
    public void loadLocalQueueWhenFetchedRemotePlayQueueIsEmpty() {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(TRACK_URN1));
        when(playQueueManager.getCurrentQueueTrackUrns()).thenReturn(PLAY_QUEUE);
        when(castQueueController.buildCastPlayQueue(any(Urn.class), any())).thenReturn(CAST_PLAY_QUEUE);
        mockProgressAndDuration(0L, 1234L);

        castPlayer.onRemoteEmptyStateFetched();

        verify(castProtocol).sendLoad(eq(TRACK_URN1.toString()),
                                      anyBoolean(),
                                      anyLong(),
                                      any(CastPlayQueue.class));
    }

    @Test
    public void slicesLocalQueueWhenLoadingInOnTheReceiver() {
        List<Urn> urns = Arrays.asList(TRACK_URN1, TRACK_URN2, TRACK_URN3);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(TestPlayQueueItem.createTrack(TRACK_URN2));
        when(playQueueManager.getCurrentQueueTrackUrns()).thenReturn(urns);
        when(castQueueController.buildCastPlayQueue(any(Urn.class), any())).thenReturn(CAST_PLAY_QUEUE);
        mockProgressAndDuration(0L, 1234L);

        castPlayer.onRemoteEmptyStateFetched();

        verify(castQueueSlicer).slice(urns, 1);
    }

    @Test
    public void seekUpdatesLocalSessionStateProvider() {
        long progress = 456789L;
        long duration = 98765567L;
        mockProgressAndDuration(progress, duration);
        when(castQueueController.getRemoteCurrentTrackUrn()).thenReturn(TRACK_URN1);
        ArgumentCaptor<Action2<Long, Long>> actionCaptor = ArgumentCaptor.forClass(Action2.class);

        castPlayer.seek(progress);

        verify(castProtocol).seek(eq(progress), actionCaptor.capture());
        actionCaptor.getValue().call(progress, duration);
        verify(playSessionStateProvider).onProgressEvent(playbackProgressEventArgumentCaptor.capture());
        assertThat(playbackProgressEventArgumentCaptor.getValue().getPlaybackProgress().getPosition()).isEqualTo(progress);
    }

    private void mockProgressAndDuration(long progress, long duration) {
        when(castProtocol.isConnected()).thenReturn(true);
        when(playSessionStateProvider.getLastProgressEvent()).thenReturn(playbackProgress);
        when(playSessionStateProvider.getLastProgressForItem(any(Urn.class))).thenReturn(playbackProgress);
        when(playbackProgress.getPosition()).thenReturn(progress);
        when(playbackProgress.getDuration()).thenReturn(duration);
    }
}
