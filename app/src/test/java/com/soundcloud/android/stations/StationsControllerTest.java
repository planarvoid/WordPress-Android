package com.soundcloud.android.stations;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class StationsControllerTest extends AndroidUnitTest {
    private static final long TRACK_ID = 123L;
    private static final Urn TRACK_URN = Urn.forTrack(TRACK_ID);
    private static final Urn STATION = Urn.forTrackStation(TRACK_ID);

    @Mock StationsOperations operations;
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() {
        new StationsController(eventBus, operations).subscribe();
    }

    @Test
    public void shouldIgnorePlaylist() {
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN, Urn.forPlaylist(123L), 0));
        verifyZeroInteractions(operations);
    }

    @Test
    public void shouldSaveCurrentTrackPositionWhenPlayingAStation() {
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN, STATION, 0));
        verify(operations).saveLastPlayedTrackPosition(STATION, 0);
    }

    @Test
    public void shouldNotSaveRecentlyPlayedStationsWhenPlayingAPlaylist() {
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue(Urn.forPlaylist(123L)));
        verifyZeroInteractions(operations);
    }

    @Test
    public void shouldOnlySaveStationsAsRecentWhenThereIsANewPlayQueueEvent() {
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromQueueUpdate(STATION));
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromAudioAdRemoved(STATION));
        verifyZeroInteractions(operations);
    }

    @Test
    public void shouldSaveStationAsRecentlyPlayedStationsWhenPlayingAStation() {
        eventBus.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromNewQueue(STATION));
        verify(operations).saveRecentlyPlayedStation(STATION);
    }
}