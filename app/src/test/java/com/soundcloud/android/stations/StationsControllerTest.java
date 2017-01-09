package com.soundcloud.android.stations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UrnStateChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class StationsControllerTest extends AndroidUnitTest {
    private static final Urn TRACK_URN = TestPlayStates.URN;
    private static final Urn STATION = Urn.forTrackStation(TRACK_URN.getNumericId());

    @Mock StationsOperations operations;

    private TestEventBus eventBus = new TestEventBus();
    private final PublishSubject<SyncJobResult> syncLikedStations = PublishSubject.create();

    @Before
    public void setUp() {
        when(operations.syncLikedStations()).thenReturn(syncLikedStations);

        new StationsController(eventBus, operations, Schedulers.immediate()).subscribe();
    }

    @Test
    public void shouldIgnorePlaylist() {
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(TRACK_URN),
                                                                       Urn.forPlaylist(123L),
                                                                       0));
        verify(operations, never()).saveLastPlayedTrackPosition(any(Urn.class), anyInt());
        verify(operations, never()).saveRecentlyPlayedStation(any(Urn.class));
    }

    @Test
    public void shouldSaveCurrentTrackPositionWhenPlayingAStation() {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.playing());
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(TRACK_URN),
                                                                       STATION,
                                                                       0));
        verify(operations).saveLastPlayedTrackPosition(STATION, 0);
    }

    @Test
    public void shouldPublisEventWhenPlayingAStation() {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.playing());
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(TRACK_URN),
                                                                       STATION,
                                                                       0));

        final UrnStateChangedEvent event = eventBus.lastEventOn(EventQueue.URN_STATE_CHANGED);
        assertThat(event.kind()).isEqualTo(UrnStateChangedEvent.Kind.STATIONS_COLLECTION_UPDATED);
        assertThat(event.urns().iterator().next()).isEqualTo(STATION);
    }

    @Test
    public void shouldNotSaveRecentlyPlayedStationsWhenStationNotPlaying() {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, TestPlayStates.buffering());
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(TRACK_URN),
                                                                       STATION,
                                                                       0));

        verify(operations, never()).saveLastPlayedTrackPosition(any(Urn.class), anyInt());
        verify(operations, never()).saveRecentlyPlayedStation(any(Urn.class));
    }

    @Test
    public void shouldNotSaveRecentlyPlayedStationsWhenPlayingAPlaylist() {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED,
                         TestPlayStates.playing());
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(TRACK_URN),
                                                                       Urn.forPlaylist(456L),
                                                                       1));

        verify(operations, never()).saveLastPlayedTrackPosition(any(Urn.class), anyInt());
        verify(operations, never()).saveRecentlyPlayedStation(any(Urn.class));
    }

    @Test
    public void shouldSaveStationAsRecentlyPlayedStationsWhenPlayingAStation() {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED,
                         TestPlayStates.playing());
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromPositionChanged(TestPlayQueueItem.createTrack(TRACK_URN),
                                                                       STATION,
                                                                       1));

        verify(operations).saveRecentlyPlayedStation(STATION);
    }

}
