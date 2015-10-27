package com.soundcloud.android.stations;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.Player;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.schedulers.Schedulers;

@RunWith(MockitoJUnitRunner.class)
public class StationsControllerTest {
    private static final long TRACK_ID = 123L;
    private static final Urn TRACK_URN = Urn.forTrack(TRACK_ID);
    private static final Urn STATION = Urn.forTrackStation(TRACK_ID);

    @Mock StationsOperations operations;
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() {
        new StationsController(eventBus, operations, Schedulers.immediate()).subscribe();
    }

    @Test
    public void shouldIgnorePlaylist() {
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(TRACK_URN), Urn.forPlaylist(123L), 0));
        verifyZeroInteractions(operations);
    }

    @Test
    public void shouldSaveCurrentTrackPositionWhenPlayingAStation() {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.PLAYING, Player.Reason.NONE, TRACK_URN));
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(TRACK_URN), STATION, 0));
        verify(operations).saveLastPlayedTrackPosition(STATION, 0);
    }

    @Test
    public void shouldNotSaveRecentlyPlayedStationsWhenStationNotPlaying() {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.BUFFERING, Player.Reason.NONE, TRACK_URN));
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(TRACK_URN), STATION, 0));

        verifyZeroInteractions(operations);
    }

    @Test
    public void shouldNotSaveRecentlyPlayedStationsWhenPlayingAPlaylist() {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.PLAYING, Player.Reason.NONE, TRACK_URN));
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(TRACK_URN), Urn.forPlaylist(456L), 1));

        verifyZeroInteractions(operations);
    }

    @Test
    public void shouldSaveStationAsRecentlyPlayedStationsWhenPlayingAStation() {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Player.StateTransition(Player.PlayerState.PLAYING, Player.Reason.NONE, TRACK_URN));
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(TRACK_URN), STATION, 1));

        verify(operations).saveRecentlyPlayedStation(STATION);
    }

    @Test
    public void shouldTriggerSyncUponLogin() {
        eventBus.publish(EventQueue.CURRENT_USER_CHANGED, CurrentUserChangedEvent.forUserUpdated(new PublicApiUser(123L, "hello")));

        verify(operations).sync();
    }
}
