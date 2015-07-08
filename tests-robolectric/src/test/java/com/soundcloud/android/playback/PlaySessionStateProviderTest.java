package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class PlaySessionStateProviderTest {

    private static final long TRACK_ID = 123L;
    private static final Urn TRACK_URN = Urn.forTrack(TRACK_ID);
    private static final PublicApiTrack TRACK = new PublicApiTrack(TRACK_ID);

    private PlaySessionStateProvider provider;
    private TestEventBus eventBus = new TestEventBus();

    @Mock
    private PlayQueueManager playQueueManager;

    @Before
    public void setUp() throws Exception {
        provider = new PlaySessionStateProvider(eventBus, playQueueManager);
        provider.subscribe();

        when(playQueueManager.getCurrentTrackId()).thenReturn(TRACK_ID);
    }


    @Test
    public void stateListenerIgnoresDefaultEvent() {
        final Playa.StateTransition lastTransition = TestPlayStates.playing();
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, lastTransition);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, Playa.StateTransition.DEFAULT);
        expect(provider.isPlaying()).toBeTrue();
    }

    @Test
    public void isPlayingTrackReturnsFalseIfNoPlayStateEventIsReceived() {
        expect(provider.isPlayingTrack(TRACK)).toBeFalse();
    }

    @Test
    public void isPlayingTrackReturnsTrueIfLastTransitionHappenedOnTheTargetTrack() {
        sendIdleStateEvent();

        expect(provider.isPlayingTrack(TRACK)).toBeTrue();
    }

    @Test
    public void isGetCurrentProgressReturns0IfCurrentTrackDidNotStartPlaying() {
        sendIdleStateEvent();

        expect(provider.getLastProgressByUrn(TRACK_URN).getPosition()).toEqual(0L);
    }

    @Test
    public void returnsLastProgressEventByUrnFromEventQueue() throws Exception {

        final PlaybackProgressEvent playbackProgressEvent = new PlaybackProgressEvent(new PlaybackProgress(1L, 2L), TRACK_URN);
        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, playbackProgressEvent);
        expect(provider.getLastProgressByUrn(TRACK_URN)).toBe(playbackProgressEvent.getPlaybackProgress());
    }

    @Test
    public void returnsEmptyProgressByUrnIfNoProgressReceived() throws Exception {
        expect(provider.getLastProgressByUrn(TRACK_URN)).toEqual(PlaybackProgress.empty());
    }

    @Test
    public void onStateTransitionForPlayStoresPlayingTrackProgress() throws Exception {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE, TRACK_URN, 1, 456));

        Urn nextTrackUrn = Urn.forTrack(321);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE, nextTrackUrn, 123, 456));

        expect(provider.getLastProgressByUrn(nextTrackUrn)).toEqual(new PlaybackProgress(123, 456));
    }

    private void sendIdleStateEvent() {
        final Playa.StateTransition lastTransition = new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.NONE, TRACK_URN);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, lastTransition);
    }

    @Test
    public void onStateTransitionForTrackEndSavesQueueWithPositionWithZero() throws Exception {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.TRACK_COMPLETE, TRACK_URN, 123, 456));
        verify(playQueueManager).saveCurrentProgress(0);
    }

    @Test
    public void onStateTransitionForReasonNoneSavesQueueWithPositionFromTransition() throws Exception {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.NONE, TRACK_URN, 123, 456));
        verify(playQueueManager).saveCurrentProgress(123);
    }

    @Test
    public void onStateTransitionWithInvalidDurationUsesPreviousPosition() throws Exception {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE, TRACK_URN, 123, 456));
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.NONE, TRACK_URN, 456, 0));
        verify(playQueueManager).saveCurrentProgress(123);
    }

    @Test
    public void onStateTransitionForBufferingDoesNotSaveProgressIfResuming() throws Exception {
        when(playQueueManager.getPlayProgressInfo()).thenReturn(new PlaybackProgressInfo(TRACK_ID, 123L));
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE, TRACK_URN, 0, 456));
        verify(playQueueManager, never()).saveCurrentProgress(anyLong());
    }

    @Test
    public void onStateTransitionForWithConsecutivePlaylistEventsSavesProgressOnTrackChange() {
        final Playa.StateTransition state1 = new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE, Urn.forTrack(1L));
        final Playa.StateTransition state2 = new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE, Urn.forTrack(2L), 123, 456);

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, state1);
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, state2);
        verify(playQueueManager).saveCurrentProgress(123);
    }

    @Test
    public void onStateTransitionShouldNotStoreCurrentProgressIfDurationIsInvalid() {
        final Playa.StateTransition state = new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE, Urn.forTrack(2L), 123, 0);

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, state);

        expect(provider.hasCurrentProgress(Urn.forTrack(2L))).toBeFalse();
    }

    @Test
    public void onStateTransitionShouldStoreCurrentProgressIfDurationIsValid() {
        final Playa.StateTransition state = new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE, Urn.forTrack(2L), 123, 1);

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, state);

        expect(provider.hasCurrentProgress(Urn.forTrack(2L))).toBeTrue();
    }
}