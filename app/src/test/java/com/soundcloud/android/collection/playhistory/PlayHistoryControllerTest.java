package com.soundcloud.android.collection.playhistory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.collection.recentlyplayed.PushRecentlyPlayedCommand;
import com.soundcloud.android.collection.recentlyplayed.WriteRecentlyPlayedCommand;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayHistoryEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayStateReason;
import com.soundcloud.android.playback.PlaybackState;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayStates;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import java.util.List;

public class PlayHistoryControllerTest extends AndroidUnitTest {
    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private static final Urn TRACK_URN2 = Urn.forTrack(234L);
    private static final Urn COLLECTION_URN = Urn.forArtistStation(987L);
    private static final long START_EVENT = 12345678L;
    private static final PlayHistoryRecord RECORD2 = PlayHistoryRecord.create(START_EVENT, TRACK_URN2, COLLECTION_URN);
    private static final PlayHistoryRecord RECORD = PlayHistoryRecord.create(START_EVENT, TRACK_URN, COLLECTION_URN);

    @Mock FeatureFlags featureFlags;
    @Mock WritePlayHistoryCommand playHistoryStoreCommand;
    @Mock WriteRecentlyPlayedCommand recentlyPlayedStoreCommand;
    @Mock PlayHistoryOperations playHistoryOperations;
    @Mock PushPlayHistoryCommand pushPlayHistoryCommand;
    @Mock private PushRecentlyPlayedCommand pushRecentlyPlayedCommand;

    private Scheduler scheduler = Schedulers.immediate();
    private TestDateProvider dateProvider = new TestDateProvider(START_EVENT);
    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        when(featureFlags.isEnabled(Flag.LOCAL_PLAY_HISTORY)).thenReturn(true);
        PlayHistoryController controller = new PlayHistoryController(eventBus,
                                                                     playHistoryStoreCommand,
                                                                     recentlyPlayedStoreCommand,
                                                                     featureFlags,
                                                                     pushPlayHistoryCommand,
                                                                     pushRecentlyPlayedCommand,
                                                                     scheduler
        );
        controller.subscribe();
    }

    @Test
    public void storesPlayHistory() {
        publishStateEvents(RECORD);

        verify(playHistoryStoreCommand).call(RECORD);
    }

    @Test
    public void storesPlayContexts() {
        publishStateEvents(RECORD);

        verify(recentlyPlayedStoreCommand).call(RECORD);
    }

    @Test
    public void emitsMultipleTimes() {
        publishStateEvents(RECORD);
        publishStateEvents(RECORD2);

        verify(playHistoryStoreCommand).call(RECORD);
        verify(recentlyPlayedStoreCommand).call(RECORD);
        verify(playHistoryStoreCommand).call(RECORD2);
        verify(recentlyPlayedStoreCommand).call(RECORD2);
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
        final TrackQueueItem item = new TrackQueueItem.Builder(trackUrn).build();
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
