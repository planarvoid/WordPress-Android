package com.soundcloud.android.stations;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class StationsControllerTest extends AndroidUnitTest {
    public static final long TRACK_ID = 123L;
    public static final Urn TRACK_URN = Urn.forTrack(TRACK_ID);
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
        Urn station = Urn.forTrackStation(TRACK_ID);
        eventBus.publish(EventQueue.PLAY_QUEUE_TRACK, CurrentPlayQueueTrackEvent.fromPositionChanged(TRACK_URN, station, 0));
        verify(operations).saveLastPlayedTrackPosition(station, 0);
    }
}