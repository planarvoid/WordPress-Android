package com.soundcloud.android.collection;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayHistoryEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayStateReason;
import com.soundcloud.android.playback.PlaybackState;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.schedulers.TestScheduler;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class PlayHistoryControllerTest extends AndroidUnitTest {
    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private static final Urn TRACK_URN2 = Urn.forTrack(234L);
    private static final Urn COLLECTION_URN = Urn.forArtistStation(987L);
    private static final long START_EVENT = 12345678L;

    @Mock WritePlayHistoryCommand storeCommand;

    private TestScheduler scheduler = new TestScheduler();
    private TestDateProvider dateProvider = new TestDateProvider(START_EVENT);
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        PlayHistoryController controller = new PlayHistoryController(eventBus, storeCommand, scheduler);
        controller.subscribe();
    }

    @Test
    public void storesLongPlaysAfterInterval() {
        publishStateEvents(TRACK_URN, COLLECTION_URN, true);

        scheduler.advanceTimeBy(1000, TimeUnit.MILLISECONDS);

        verify(storeCommand, times(1)).call(PlayHistoryRecord.create(START_EVENT, TRACK_URN, COLLECTION_URN));
    }

    @Test
    public void doesNotStoreShortPlays() {
        publishStateEvents(TRACK_URN, COLLECTION_URN, true);

        scheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS);

        verify(storeCommand, never()).call(PlayHistoryRecord.create(START_EVENT, TRACK_URN, COLLECTION_URN));
    }

    @Test
    public void resetsOnNewTrack() {
        publishStateEvents(TRACK_URN, COLLECTION_URN, true);
        scheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS);

        publishStateEvents(TRACK_URN2, COLLECTION_URN, true);
        scheduler.advanceTimeBy(1000, TimeUnit.MILLISECONDS);

        verify(storeCommand, times(1)).call(PlayHistoryRecord.create(START_EVENT, TRACK_URN2, COLLECTION_URN));
    }

    @Test
    public void emitsMultipleTimes() {
        publishStateEvents(TRACK_URN, COLLECTION_URN, true);
        scheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS);

        publishStateEvents(TRACK_URN2, COLLECTION_URN, true);
        scheduler.advanceTimeBy(1000, TimeUnit.MILLISECONDS);

        verify(storeCommand, times(1)).call(PlayHistoryRecord.create(START_EVENT, TRACK_URN2, COLLECTION_URN));
    }

    @Test
    public void publishesPlayHistoryAddedEvent() {
        publishStateEvents(TRACK_URN, COLLECTION_URN, true);

        scheduler.advanceTimeBy(1000, TimeUnit.MILLISECONDS);

        List<PlayHistoryEvent> playHistoryEvents = eventBus.eventsOn(EventQueue.PLAY_HISTORY);
        assertThat(playHistoryEvents.size()).isEqualTo(1);
        assertThat(playHistoryEvents.get(0)).isEqualTo(PlayHistoryEvent.fromAdded(TRACK_URN));
    }

    private void publishStateEvents(Urn trackUrn, Urn collectionUrn, boolean playing) {
        final TrackQueueItem item = new TrackQueueItem.Builder(trackUrn).build();
        final PlaybackState playbackState = playing ? PlaybackState.PLAYING : PlaybackState.IDLE;

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED,
                         new PlaybackStateTransition(playbackState,
                                                     PlayStateReason.NONE,
                                                     trackUrn,
                                                     0,
                                                     1000,
                                                     dateProvider));
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromNewQueue(item, collectionUrn, 0));
    }

}
