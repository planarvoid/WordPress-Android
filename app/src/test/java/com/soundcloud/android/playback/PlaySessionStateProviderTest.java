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
import com.soundcloud.android.utils.UuidProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class PlaySessionStateProviderTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = TestPlayStates.URN;

    private PlaySessionStateProvider provider;

    @Mock private PlaySessionStateStorage playSessionStateStorage;
    @Mock private UuidProvider uuidProvider;
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
        final PlaybackStateTransition lastTransition = TestPlayStates.playing();
        provider.onPlayStateTransition(lastTransition);
        provider.onPlayStateTransition(PlaybackStateTransition.DEFAULT);
        assertThat(provider.isPlaying()).isTrue();
    }

    @Test
    public void isInErrorStateReturnsTrueIfLastTransitionWasError() {
        final PlaybackStateTransition lastTransition = new PlaybackStateTransition(PlaybackState.IDLE,
                                                                                   PlayStateReason.ERROR_FAILED,
                                                                                   TRACK_URN);
        provider.onPlayStateTransition(lastTransition);

        assertThat(provider.isInErrorState()).isTrue();
    }

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
        provider.onPlayStateTransition(createStateTransition(PlaybackState.PLAYING,
                                                             PlayStateReason.NONE,
                                                             TRACK_URN,
                                                             1,
                                                             456));

        Urn nextTrackUrn = Urn.forTrack(321);
        provider.onPlayStateTransition(createStateTransition(PlaybackState.PLAYING,
                                                             PlayStateReason.NONE,
                                                             nextTrackUrn,
                                                             123,
                                                             456));

        assertThat(provider.getLastProgressForItem(nextTrackUrn)).isEqualTo(createPlaybackProcess(123, 456));
    }

    private void sendIdleStateEvent() {
        final PlaybackStateTransition lastTransition = new PlaybackStateTransition(PlaybackState.IDLE,
                                                                                   PlayStateReason.NONE,
                                                                                   TRACK_URN);
        provider.onPlayStateTransition(lastTransition);
    }

    @Test
    public void onStateTransitionForItemEndSavesQueueWithPositionWithZero() throws Exception {
        provider.onPlayStateTransition(createStateTransition(PlaybackState.IDLE,
                                                             PlayStateReason.PLAYBACK_COMPLETE,
                                                             TRACK_URN,
                                                             123,
                                                             456));
        verify(playSessionStateStorage).saveProgress(123);
    }

    @Test
    public void onStateTransitionForReasonNoneSavesQueueWithPositionFromTransition() throws Exception {
        provider.onPlayStateTransition(createStateTransition(PlaybackState.IDLE,
                                                             PlayStateReason.NONE,
                                                             TRACK_URN,
                                                             123,
                                                             456));
        verify(playSessionStateStorage).saveProgress(123);
    }

    @Test
    public void onStateTransitionForBufferingDoesNotSaveProgressIfResuming() throws Exception {
        when(playSessionStateStorage.getLastPlayingItem()).thenReturn(TRACK_URN);
        when(playSessionStateStorage.getLastStoredProgress()).thenReturn(123L);
        provider.onPlayStateTransition(createStateTransition(PlaybackState.BUFFERING,
                                                             PlayStateReason.NONE,
                                                             TRACK_URN,
                                                             0,
                                                             456));
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

        provider.onPlayStateTransition(state1);
        provider.onPlayStateTransition(state2);
        verify(playSessionStateStorage).saveProgress(123);
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
