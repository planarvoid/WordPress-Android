package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Consts;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.android.utils.TestDateProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class PlaySessionStateProviderTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = TestPlayStates.URN;

    private PlaySessionStateProvider provider;

    @Mock private PlaySessionStateStorage playSessionStateStorage;
    @Mock private PlayQueueManager playQueueManager;
    private TestDateProvider dateProvider;

    @Before
    public void setUp() throws Exception {
        provider = new PlaySessionStateProvider(playSessionStateStorage);

        dateProvider = new TestDateProvider();

        when(playSessionStateStorage.getLastPlayingItem()).thenReturn(TRACK_URN);
    }

    @Test
    public void stateListenerIgnoresDefaultEvent() {
        provider.onPlayStateTransition(TestPlayStates.playing());
        provider.onPlayStateTransition(PlayStateEvent.DEFAULT);
        assertThat(provider.isPlaying()).isTrue();
    }

    @Test
    public void isInErrorStateReturnsTrueIfLastTransitionWasError() {
        provider.onPlayStateTransition(TestPlayStates.error(PlayStateReason.ERROR_FAILED));

        assertThat(provider.isInErrorState()).isTrue();
    }

    @Test
    public void isCurrentlyPlayingTrueIfReceivedEventForTrack() {
        provider.onPlayStateTransition(TestPlayStates.buffering());

        assertThat(provider.isCurrentlyPlaying(TestPlayStates.URN)).isTrue();
    }

    @Test
    public void isCurrentlyPlayingFalseIfReceivedEventForTrack() {
        provider.onPlayStateTransition(TestPlayStates.buffering());

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
        provider.onPlayStateTransition(TestPlayStates.playing());

        final PlaybackProgressEvent playbackProgressEvent = PlaybackProgressEvent.create(createPlaybackProcess(1L, 2L),
                                                                                         TRACK_URN);
        provider.onProgressEvent(playbackProgressEvent);
        assertThat(provider.getLastProgressForItem(TRACK_URN)).isSameAs(playbackProgressEvent.getPlaybackProgress());
    }

    @Test
    public void returnsSavedProgressByUrnIfNoProgressReceivedYet() throws Exception {
        when(playSessionStateStorage.getLastStoredProgress()).thenReturn(123L);
        assertThat(provider.getLastProgressForItem(TRACK_URN)).isEqualTo(new PlaybackProgress(123, Consts.NOT_SET));
    }

    @Test
    public void returnsEmptyProgressByUrnIfNoProgressReceived() throws Exception {
        when(playSessionStateStorage.getLastPlayingItem()).thenReturn(Urn.forTrack(987654));

        assertThat(provider.getLastProgressForItem(TRACK_URN)).isEqualTo(PlaybackProgress.empty());
    }

    @Test
    public void onStateTransitionForPlayStoresPlayingItemProgress() throws Exception {
        provider.onPlayStateTransition(TestPlayStates.playing(1, 456));

        Urn nextTrackUrn = Urn.forTrack(321);
        provider.onPlayStateTransition(TestPlayStates.playing(nextTrackUrn, 123, 456, dateProvider));

        assertThat(provider.getLastProgressForItem(nextTrackUrn)).isEqualTo(createPlaybackProcess(123, 456));
    }

    private void sendIdleStateEvent() {
        provider.onPlayStateTransition(TestPlayStates.idle());
    }

    @Test
    public void onStateTransitionForItemEndSavesQueueWithPositionWithZero() throws Exception {
        provider.onPlayStateTransition(TestPlayStates.complete());
        verify(playSessionStateStorage).saveProgress(0);
    }

    @Test
    public void onStateTransitionForItemEndWillReportItemNotPlayingAfter() throws Exception {
        provider.onPlayStateTransition(TestPlayStates.complete());
        assertThat(provider.isCurrentlyPlaying(TRACK_URN)).isFalse();
    }

    @Test
    public void onStateTransitionForReasonNoneSavesQueueWithPositionFromTransition() throws Exception {
        provider.onPlayStateTransition(TestPlayStates.idle(123, 456));
        verify(playSessionStateStorage).saveProgress(123);
    }

    @Test
    public void onStateTransitionForBufferingDoesNotSaveProgressIfResuming() throws Exception {
        when(playSessionStateStorage.getLastStoredProgress()).thenReturn(123L);

        provider.onPlayStateTransition(TestPlayStates.buffering());

        verify(playSessionStateStorage, never()).saveProgress(anyLong());
    }

    @Test
    public void onStateTransitionForWithConsecutivePlaylistEventsSavesProgressOnTrackChange() {
        final PlaybackStateTransition state1 = new PlaybackStateTransition(PlaybackState.PLAYING,
                                                                           PlayStateReason.NONE,
                                                                           Urn.forTrack(1L));
        final PlaybackStateTransition state2 = createStateTransition(PlaybackState.PLAYING,
                                                                     PlayStateReason.NONE,
                                                                     Urn.forTrack(2L),
                                                                     123,
                                                                     456);

        provider.onPlayStateTransition(TestPlayStates.playing(Urn.forTrack(1), 12, 456));
        provider.onPlayStateTransition(TestPlayStates.playing(Urn.forTrack(2), 34, 456));
        verify(playSessionStateStorage).saveProgress(34);
    }

    private PlaybackStateTransition createStateTransition(PlaybackState playing,
                                                          PlayStateReason none,
                                                          Urn trackUrn,
                                                          int currentProgress,
                                                          int duration) {
        return new PlaybackStateTransition(playing, none, trackUrn, currentProgress, duration, dateProvider);
    }

    private PlaybackProgress createPlaybackProcess(long position, long duration) {
        return new PlaybackProgress(position, duration, dateProvider);
    }

}
