package com.soundcloud.android.collection;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

import com.soundcloud.android.collection.playhistory.PlayHistoryOperations;
import com.soundcloud.android.collection.playhistory.PlayHistoryStorage;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedItem;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.Scheduler;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.Collections;
import java.util.List;

public class PlayHistoryOperationsTest extends AndroidUnitTest {

    private static final List<TrackItem> TRACK_ITEMS = ModelFixtures.trackItems(10);

    private static final RecentlyPlayedItem RECENTLY_PLAYED_ITEM =
            RecentlyPlayedItem.create(Urn.forPlaylist(123L), Optional.<String>absent(), "title", 5, false);

    private static final List<RecentlyPlayedItem> RECENTLY_PLAYED_ITEMS =
            Collections.singletonList(RECENTLY_PLAYED_ITEM);

    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private PlayHistoryStorage playHistoryStorage;

    private Scheduler scheduler = Schedulers.immediate();

    private TestSubscriber<List<TrackItem>> trackSubscriber;

    private PlayHistoryOperations operations;

    @Before
    public void setUp() throws Exception {
        when(playHistoryStorage.fetchTracks(anyInt())).thenReturn(Observable.from(TRACK_ITEMS));
        trackSubscriber = new TestSubscriber<>();
        operations = new PlayHistoryOperations(playbackInitiator, playHistoryStorage, scheduler);
    }

    @Test
    public void playHistoryReturnsAllTracksWhenNoLimitIsSpecified() throws Exception {
        operations.playHistory().subscribe(trackSubscriber);

        trackSubscriber.assertValue(TRACK_ITEMS);
        trackSubscriber.assertCompleted();
    }

    @Test
    public void recentlyPlayedReturnsListOfRecentlyPlayedItemsWithLimit() {
        TestSubscriber<List<RecentlyPlayedItem>> recentlyPlayedSubscriber = new TestSubscriber<>();
        when(playHistoryStorage.fetchContexts(4))
                .thenReturn(Observable.from(RECENTLY_PLAYED_ITEMS));

        operations.recentlyPlayed(4).subscribe(recentlyPlayedSubscriber);

        recentlyPlayedSubscriber.assertValue(RECENTLY_PLAYED_ITEMS);
        recentlyPlayedSubscriber.assertCompleted();
    }

    @Test
    public void recentlyPlayedReturnsListOfRecentlyPlayedItemsWithMaxLimit() {
        TestSubscriber<RecentlyPlayedItem> recentlyPlayedSubscriber = new TestSubscriber<>();
        when(playHistoryStorage.fetchContexts(PlayHistoryOperations.MAX_RECENTLY_PLAYED))
                .thenReturn(Observable.from(RECENTLY_PLAYED_ITEMS));

        operations.recentlyPlayed().subscribe(recentlyPlayedSubscriber);

        recentlyPlayedSubscriber.assertValueCount(RECENTLY_PLAYED_ITEMS.size());
        recentlyPlayedSubscriber.assertCompleted();
    }

}
