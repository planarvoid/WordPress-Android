package com.soundcloud.android.collection.playhistory;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.collection.recentlyplayed.PushRecentlyPlayedCommand;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedStorage;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayHistoryEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayStateReason;
import com.soundcloud.android.playback.PlaybackState;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.rx.eventbus.TestEventBusV2;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class PlayHistoryControllerTest {
    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private static final Urn TRACK_URN2 = Urn.forTrack(234L);
    private static final Urn COLLECTION_URN = Urn.forArtistStation(987L);
    private static final long START_EVENT = 12345678L;
    private static final PlayHistoryRecord RECORD2 = PlayHistoryRecord.create(START_EVENT, TRACK_URN2, COLLECTION_URN);
    private static final PlayHistoryRecord RECORD = PlayHistoryRecord.create(START_EVENT, TRACK_URN, COLLECTION_URN);

    @Mock PlayHistoryStorage playHistoryStorage;
    @Mock RecentlyPlayedStorage recentlyPlayedStorage;
    @Mock PlayHistoryOperations playHistoryOperations;
    @Mock PushPlayHistoryCommand pushPlayHistoryCommand;
    @Mock private PushRecentlyPlayedCommand pushRecentlyPlayedCommand;

    private Scheduler scheduler = Schedulers.trampoline();
    private TestDateProvider dateProvider = new TestDateProvider(START_EVENT);
    private TestEventBusV2 eventBus = new TestEventBusV2();

    @Before
    public void setUp() throws Exception {
        PlayHistoryController controller = new PlayHistoryController(eventBus,
                                                                     playHistoryStorage,
                                                                     recentlyPlayedStorage,
                                                                     pushPlayHistoryCommand,
                                                                     pushRecentlyPlayedCommand,
                                                                     scheduler);
        controller.subscribe();
    }

    @Test
    public void storesPlayHistory() {
        publishStateEvents(RECORD);

        verify(playHistoryStorage).upsertRow(RECORD);
    }

    @Test
    public void storesPlayContexts() {
        publishStateEvents(RECORD);

        verify(recentlyPlayedStorage).upsertRow(RECORD);
    }

    @Test
    public void doesntStoreRecentlyPlayedForInvalidContextUrn() {
        final PlayHistoryRecord playHistoryRecord = PlayHistoryRecord.create(START_EVENT, TRACK_URN, Urn.NOT_SET);
        publishStateEvents(playHistoryRecord);

        verify(recentlyPlayedStorage, never()).upsertRow(playHistoryRecord);
    }

    @Test
    public void emitsMultipleTimes() {
        publishStateEvents(RECORD);
        publishStateEvents(RECORD2);

        verify(playHistoryStorage).upsertRow(RECORD);
        verify(recentlyPlayedStorage).upsertRow(RECORD);
        verify(playHistoryStorage).upsertRow(RECORD2);
        verify(recentlyPlayedStorage).upsertRow(RECORD2);
    }

    @Test
    public void publishesPlayHistoryAddedEvent() {
        publishStateEvents(RECORD);

        List<PlayHistoryEvent> playHistoryEvents = eventBus.eventsOn(EventQueue.PLAY_HISTORY);
        assertThat(playHistoryEvents.size()).isEqualTo(1);
        assertThat(playHistoryEvents.get(0)).isEqualTo(PlayHistoryEvent.fromAdded(TRACK_URN));
    }

    @Test
    public void pushesPlayHistoryToServer() {
        publishStateEvents(RECORD);

        verify(pushPlayHistoryCommand).call(RECORD);
    }

    private void publishStateEvents(PlayHistoryRecord record) {
        final Urn trackUrn = record.trackUrn();
        final Urn collectionUrn = record.contextUrn();
        final TrackQueueItem item = TestPlayQueueItem.createTrack(trackUrn);
        final PlaybackState playbackState = PlaybackState.PLAYING;

        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED,
                         TestPlayStates.wrap(new PlaybackStateTransition(playbackState,
                                                                         PlayStateReason.NONE,
                                                                         trackUrn,
                                                                         0,
                                                                         1000,
                                                                         dateProvider)));
        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                         CurrentPlayQueueItemEvent.fromNewQueue(item, collectionUrn, 0));
    }

}
