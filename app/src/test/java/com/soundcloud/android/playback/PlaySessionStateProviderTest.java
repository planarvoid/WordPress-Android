package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.android.testsupport.fixtures.TestPlayerTransitions;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.android.utils.UuidProvider;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.concurrent.TimeUnit;

public class PlaySessionStateProviderTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = TestPlayStates.URN;
    private static final int DURATION = 1000;
    private static final String RANDOM_UUID = "random-uuid";
    private static final String LAST_PLAY_ID = "last-play-id";

    private PlaySessionStateProvider provider;

    @Mock private PlaySessionStateStorage playSessionStateStorage;
    @Mock private UuidProvider uuidProvider;
    @Mock private PlayQueueManager playQueueManager;
    private TestEventBus eventBus = new TestEventBus();
    private TestDateProvider dateProvider;

    @Before
    public void setUp() throws Exception {
        dateProvider = new TestDateProvider();
        provider = new PlaySessionStateProvider(playSessionStateStorage, uuidProvider, eventBus, dateProvider);

        when(playSessionStateStorage.getLastPlayId()).thenReturn(LAST_PLAY_ID);
        when(uuidProvider.getRandomUuid()).thenReturn(RANDOM_UUID);
        when(playSessionStateStorage.getLastPlayingItem()).thenReturn(TRACK_URN);
    }

    @Test
    public void onPlayStateTransitionDoesNotSavePlayInfoForSameTrack() {
        provider.onPlayStateTransition(TestPlayerTransitions.playing(Urn.forTrack(123)), DURATION);
        verify(playSessionStateStorage, never()).savePlayInfo(any(Urn.class));
    }

    @Test
    public void onPlayStateTransitionSavesNewPlayInfoForNewTrack() {
        provider.onPlayStateTransition(TestPlayerTransitions.playing(Urn.forTrack(456)), DURATION);
        verify(playSessionStateStorage).savePlayInfo(Urn.forTrack(456));
    }

    @Test
    public void onPlayStateTransitionDoesNotCreateNewPlayForInitialBufferingEvent() {
        final PlayStateEvent playStateEvent = provider.onPlayStateTransition(TestPlayerTransitions.buffering(),
                                                                             DURATION);
        assertThat(playStateEvent.getPlayId()).isEqualTo(LAST_PLAY_ID);
        assertThat(playStateEvent.isFirstPlay()).isEqualTo(false);
        verify(playSessionStateStorage, never()).savePlayId(any(String.class));
    }

    @Test
    public void onPlayStateTransitionDoesNotCreateNewPlayIfPlayIdAlreadyExists() {
        when(playSessionStateStorage.hasPlayId()).thenReturn(true);
        final PlayStateEvent playStateEvent = provider.onPlayStateTransition(TestPlayerTransitions.buffering(),
                                                                             DURATION);
        assertThat(playStateEvent.getPlayId()).isEqualTo(LAST_PLAY_ID);
        assertThat(playStateEvent.isFirstPlay()).isEqualTo(false);
        verify(playSessionStateStorage, never()).savePlayId(any(String.class));
    }

    @Test
    public void onPlayStateTransitionCreatesNewPlayForFirstPlayWithNoLastPlayId() {
        when(playSessionStateStorage.hasPlayId()).thenReturn(false);
        final PlayStateEvent playStateEvent = provider.onPlayStateTransition(TestPlayerTransitions.playing(), DURATION);
        assertThat(playStateEvent.getPlayId()).isEqualTo(RANDOM_UUID);
        assertThat(playStateEvent.isFirstPlay()).isEqualTo(true);
    }

    @Test
    public void onPlayStateTransitionSavesNewPlayId() {
        when(playSessionStateStorage.hasPlayId()).thenReturn(false);
        provider.onPlayStateTransition(TestPlayerTransitions.playing(), DURATION);
        verify(playSessionStateStorage).savePlayId(RANDOM_UUID);
    }

    @Test
    public void isInErrorStateReturnsTrueIfLastTransitionWasError() {
        provider.onPlayStateTransition(TestPlayerTransitions.error(PlayStateReason.ERROR_FAILED), DURATION);

        assertThat(provider.isInErrorState()).isTrue();
    }

    @Test
    public void wasLastStateACastDisconnectionReturnsTrueIfLastTransitionWasThat() {
        provider.onPlayStateTransition(TestPlayerTransitions.idle(PlayStateReason.CAST_DISCONNECTED), DURATION);

        assertThat(provider.wasLastStateACastDisconnection()).isTrue();
    }

    @Test
    public void isCurrentlyPlayingTrueIfReceivedEventForTrack() {
        provider.onPlayStateTransition(TestPlayerTransitions.buffering(), DURATION);

        assertThat(provider.isCurrentlyPlaying(TestPlayStates.URN)).isTrue();
    }

    @Test
    public void isCurrentlyPlayingFalseIfReceivedEventForTrack() {
        provider.onPlayStateTransition(TestPlayerTransitions.buffering(), DURATION);

        assertThat(provider.isCurrentlyPlaying(Urn.forTrack(777))).isFalse();
    }

    @Test
    public void isLastPlayedTrueIfStorageReturnsSameTrack() {
        assertThat(provider.isLastPlayed(TRACK_URN)).isTrue();
    }

    @Test
    public void isLastPlayedFalseIfStorageReturnsDifferentTrack() {
        when(playSessionStateStorage.getLastPlayingItem()).thenReturn(Urn.forTrack(777));

        assertThat(provider.isLastPlayed(TRACK_URN)).isFalse();
    }

    @Test
    public void isGetCurrentProgressReturns0IfCurrentItemDidNotStartPlaying() {
        sendIdleStateEvent();

        assertThat(provider.getLastProgressForItem(TRACK_URN).getPosition()).isEqualTo(0L);
    }

    @Test
    public void returnsLastProgressEventByUrnFromEventQueue() throws Exception {
        provider.onPlayStateTransition(TestPlayerTransitions.playing(), DURATION);

        final PlaybackProgressEvent playbackProgressEvent = PlaybackProgressEvent.create(createPlaybackProcess(1L, 2L), TestPlayerTransitions.URN);

        provider.onProgressEvent(playbackProgressEvent);

        assertThat(provider.getLastProgressForItem(TestPlayerTransitions.URN)).isSameAs(playbackProgressEvent.getPlaybackProgress());
    }

    @Test
    public void publishesProgressChangeToEventBusWhenEventIsReceived() {
        provider.onPlayStateTransition(TestPlayerTransitions.playing(), DURATION);
        long progress = 1L;
        long duration = 2L;
        Urn urn = TestPlayerTransitions.URN;
        final PlaybackProgressEvent playbackProgressEvent = PlaybackProgressEvent.create(createPlaybackProcess(progress, duration), urn);

        provider.onProgressEvent(playbackProgressEvent);

        assertThat(eventBus.eventsOn(EventQueue.PLAYBACK_PROGRESS).size()).isEqualTo(1);
        PlaybackProgress playbackProgress = eventBus.lastEventOn(EventQueue.PLAYBACK_PROGRESS).getPlaybackProgress();
        assertThat(playbackProgress.getPosition()).isEqualTo(progress);
        assertThat(playbackProgress.getDuration()).isEqualTo(duration);
        assertThat(playbackProgress.getUrn()).isEqualTo(urn);
    }

    @Test
    public void returnsSavedProgressByUrnIfNoProgressReceivedYet() throws Exception {
        when(playSessionStateStorage.getLastStoredProgress()).thenReturn(123L);
        when(playSessionStateStorage.getLastStoredDuration()).thenReturn(456L);
        assertThat(provider.getLastProgressForItem(TRACK_URN)).isEqualTo(new PlaybackProgress(123, 456, TRACK_URN));
    }

    @Test
    public void returnsEmptyProgressByUrnIfNoProgressReceived() throws Exception {
        when(playSessionStateStorage.getLastPlayingItem()).thenReturn(Urn.forTrack(987654));

        assertThat(provider.getLastProgressForItem(TRACK_URN)).isEqualTo(PlaybackProgress.empty());
    }

    @Test
    public void onStateTransitionForPlayStoresPlayingItemProgress() throws Exception {
        provider.onPlayStateTransition(TestPlayerTransitions.playing(1, 456), DURATION);

        Urn nextTrackUrn = Urn.forTrack(321);
        provider.onPlayStateTransition(TestPlayerTransitions.playing(nextTrackUrn, 123, 456, dateProvider), DURATION);

        assertThat(provider.getLastProgressForItem(nextTrackUrn)).isEqualTo(createPlaybackProcess(123, 456));
    }

    private void sendIdleStateEvent() {
        provider.onPlayStateTransition(TestPlayerTransitions.idle(), DURATION);
    }

    @Test
    public void onStateTransitionForItemEndSavesQueueWithPositionWithZero() throws Exception {
        provider.onPlayStateTransition(TestPlayerTransitions.complete(), DURATION);
        verify(playSessionStateStorage).saveProgress(0, 0);
    }

    @Test
    public void onStateTransitionForItemEndWillReportItemNotPlayingAfter() throws Exception {
        provider.onPlayStateTransition(TestPlayerTransitions.complete(), DURATION);
        assertThat(provider.isCurrentlyPlaying(TRACK_URN)).isFalse();
    }

    @Test
    public void onStateTransitionForReasonNoneSavesQueueWithPositionFromTransition() throws Exception {
        provider.onPlayStateTransition(TestPlayerTransitions.idle(123, 456), DURATION);
        verify(playSessionStateStorage).saveProgress(123, 456);
    }

    @Test
    public void onStateTransitionForBufferingDoesNotSaveProgressIfResuming() throws Exception {
        when(playSessionStateStorage.getLastStoredProgress()).thenReturn(123L);

        provider.onPlayStateTransition(TestPlayerTransitions.buffering(), DURATION);

        verify(playSessionStateStorage, never()).saveProgress(anyLong(), anyLong());
    }

    @Test
    public void onStateTransitionForWithConsecutivePlaylistEventsSavesProgressOnTrackChange() {
        provider.onPlayStateTransition(TestPlayerTransitions.playing(Urn.forTrack(1), 12, 456), DURATION);
        provider.onPlayStateTransition(TestPlayerTransitions.playing(Urn.forTrack(2), 34, 456), DURATION);
        verify(playSessionStateStorage).saveProgress(34, 456);
    }

    @Test
    public void getMillisSinceLastPlaySessionReturnsTimeSinceLastEventWhenIsNotInPlayingState() {
        provider.onPlayStateTransition(TestPlayerTransitions.playing(Urn.forTrack(1), 12, 456), DURATION);
        dateProvider.advanceBy(10, TimeUnit.SECONDS);

        provider.onPlayStateTransition(TestPlayerTransitions.idle(), DURATION);
        dateProvider.advanceBy(10, TimeUnit.SECONDS);

        assertThat(provider.getMillisSinceLastPlaySession()).isEqualTo(TimeUnit.SECONDS.toMillis(10));
    }

    @Test
    public void getMillisSinceLastPlaySessionReturnsZeroWhenStillInPlayingState() {
        provider.onPlayStateTransition(TestPlayerTransitions.playing(Urn.forTrack(1), 12, 456), DURATION);
        dateProvider.advanceBy(10, TimeUnit.SECONDS);

        provider.onPlayStateTransition(TestPlayerTransitions.playing(Urn.forTrack(2), 34, 456), DURATION);
        dateProvider.advanceBy(10, TimeUnit.SECONDS);

        assertThat(provider.getMillisSinceLastPlaySession()).isEqualTo(0L);
    }

    private PlaybackProgress createPlaybackProcess(long position, long duration) {
        return new PlaybackProgress(position, duration, dateProvider, TRACK_URN);
    }

}
