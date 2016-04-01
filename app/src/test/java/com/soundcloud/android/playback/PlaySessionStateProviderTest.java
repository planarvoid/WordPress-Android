package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class PlaySessionStateProviderTest extends AndroidUnitTest {

    private static final long TRACK_ID = 123L;
    private static final Urn TRACK_URN = Urn.forTrack(TRACK_ID);
    private static final PublicApiTrack TRACK = new PublicApiTrack(TRACK_ID);

    private PlaySessionStateProvider provider;
    private TestEventBus eventBus = new TestEventBus();

    @Mock private PlayQueueManager playQueueManager;
    private TestDateProvider dateProvider;

    @Before
    public void setUp() throws Exception {
        provider = new PlaySessionStateProvider(eventBus, playQueueManager);
        provider.subscribe();

        dateProvider = new TestDateProvider();
    }

    @Test
    public void isPlayingCurrentPlayQueueItemReturnsFalseOnEmptyPQ() {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PlayQueueItem.EMPTY);
        assertThat(provider.isPlayingCurrentPlayQueueItem()).isFalse();
    }

    @Test
    public void stateListenerIgnoresDefaultEvent() {
        final PlaybackStateTransition lastTransition = TestPlayStates.playing();
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, lastTransition);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, PlaybackStateTransition.DEFAULT);
        assertThat(provider.isPlaying()).isTrue();
    }

    @Test
    public void isInErrorStateReturnsTrueIfLastTransitionWasError() {
        final PlaybackStateTransition lastTransition = new PlaybackStateTransition(PlaybackState.IDLE, PlayStateReason.ERROR_FAILED, TRACK_URN);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, lastTransition);

        assertThat(provider.isInErrorState()).isTrue();
    }

    public void isGetCurrentProgressReturns0IfCurrentItemDidNotStartPlaying() {
        sendIdleStateEvent();

        assertThat(provider.getLastProgressForItem(TRACK_URN).getPosition()).isEqualTo(0L);
    }

    @Test
    public void returnsLastProgressEventByUrnFromEventQueue() throws Exception {
        final PlaybackProgressEvent playbackProgressEvent = PlaybackProgressEvent.create(createPlaybackProcess(1L, 2L), TRACK_URN);
        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, playbackProgressEvent);
        assertThat(provider.getLastProgressForItem(TRACK_URN)).isSameAs(playbackProgressEvent.getPlaybackProgress());
    }

    @Test
    public void returnsEmptyProgressByUrnIfNoProgressReceived() throws Exception {
        assertThat(provider.getLastProgressForItem(TRACK_URN)).isEqualTo(PlaybackProgress.empty());
    }

    @Test
    public void onStateTransitionForPlayStoresPlayingItemProgress() throws Exception {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, createStateTransition(PlaybackState.PLAYING, PlayStateReason.NONE, TRACK_URN, 1, 456));

        Urn nextTrackUrn = Urn.forTrack(321);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, createStateTransition(PlaybackState.PLAYING, PlayStateReason.NONE, nextTrackUrn, 123, 456));

        assertThat(provider.getLastProgressForItem(nextTrackUrn)).isEqualTo(createPlaybackProcess(123, 456));
    }

    private void sendIdleStateEvent() {
        final PlaybackStateTransition lastTransition = new PlaybackStateTransition(PlaybackState.IDLE, PlayStateReason.NONE, TRACK_URN);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, lastTransition);
    }

    @Test
    public void onStateTransitionForItemEndSavesQueueWithPositionWithZero() throws Exception {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, createStateTransition(PlaybackState.IDLE, PlayStateReason.PLAYBACK_COMPLETE, TRACK_URN, 123, 456));
        verify(playQueueManager).saveCurrentProgress(0);
    }

    @Test
    public void onStateTransitionForReasonNoneSavesQueueWithPositionFromTransition() throws Exception {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, createStateTransition(PlaybackState.IDLE, PlayStateReason.NONE, TRACK_URN, 123, 456));
        verify(playQueueManager).saveCurrentProgress(123);
    }

    @Test
    public void onStateTransitionWithInvalidDurationUsesPreviousPosition() throws Exception {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, createStateTransition(PlaybackState.PLAYING, PlayStateReason.NONE, TRACK_URN, 123, 456));
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, createStateTransition(PlaybackState.IDLE, PlayStateReason.NONE, TRACK_URN, 456, 0));
        verify(playQueueManager).saveCurrentProgress(123);
    }

    @Test
    public void onStateTransitionForBufferingDoesNotSaveProgressIfResuming() throws Exception {
        when(playQueueManager.wasLastSavedItem(TRACK_URN)).thenReturn(true);
        when(playQueueManager.getLastSavedProgressPosition()).thenReturn(123L);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, createStateTransition(PlaybackState.BUFFERING, PlayStateReason.NONE, TRACK_URN, 0, 456));
        verify(playQueueManager, never()).saveCurrentProgress(anyLong());
    }

    @Test
    public void onStateTransitionForWithConsecutivePlaylistEventsSavesProgressOnTrackChange() {
        final PlaybackStateTransition state1 = new PlaybackStateTransition(PlaybackState.PLAYING, PlayStateReason.NONE, Urn.forTrack(1L));
        final PlaybackStateTransition state2 = createStateTransition(PlaybackState.PLAYING, PlayStateReason.NONE, Urn.forTrack(2L), 123, 456);

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, state1);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, state2);
        verify(playQueueManager).saveCurrentProgress(123);
    }

    @Test
    public void onStateTransitionShouldNotStoreCurrentProgressIfDurationIsInvalid() {
        final PlaybackStateTransition state = createStateTransition(PlaybackState.PLAYING, PlayStateReason.NONE, Urn.forTrack(2L), 123, 0);

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, state);

        assertThat(provider.hasLastKnownProgress(Urn.forTrack(2L))).isFalse();
    }

    @Test
    public void onStateTransitionShouldStoreCurrentProgressIfDurationIsValid() {
        final PlaybackStateTransition state = createStateTransition(PlaybackState.PLAYING, PlayStateReason.NONE, Urn.forTrack(2L), 123, 1);

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, state);

        assertThat(provider.hasLastKnownProgress(Urn.forTrack(2L))).isTrue();
    }

    private PlaybackStateTransition createStateTransition(PlaybackState playing, PlayStateReason none, Urn trackUrn, int currentProgress, int duration) {
        return new PlaybackStateTransition(playing, none, trackUrn, currentProgress, duration, dateProvider);
    }

    private PlaybackProgress createPlaybackProcess(long position, long duration) {
        return new PlaybackProgress(position, duration, dateProvider);
    }

}
