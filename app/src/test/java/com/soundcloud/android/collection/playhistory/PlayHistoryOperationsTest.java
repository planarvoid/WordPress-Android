package com.soundcloud.android.collection.playhistory;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedItem;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.sync.SyncOperations;
import com.soundcloud.android.sync.Syncable;
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
    @Mock private SyncOperations syncOperations;

    private Scheduler scheduler = Schedulers.immediate();
    private TestSubscriber<List<TrackItem>> trackSubscriber;
    private TestSubscriber<RecentlyPlayedItem> contextsSubscriber;

    private PlayHistoryOperations operations;

    @Before
    public void setUp() throws Exception {
        when(playHistoryStorage.loadTracks(anyInt())).thenReturn(Observable.from(TRACK_ITEMS));
        when(playHistoryStorage.loadContexts(anyInt())).thenReturn(Observable.from(RECENTLY_PLAYED_ITEMS));
        trackSubscriber = new TestSubscriber<>();
        contextsSubscriber = new TestSubscriber<>();
        operations = new PlayHistoryOperations(playbackInitiator, playHistoryStorage, scheduler, syncOperations);
    }

    @Test
    public void shouldReturnPlayHistory() throws Exception {
        when(syncOperations.lazySyncIfStale(Syncable.PLAY_HISTORY))
                .thenReturn(Observable.just(SyncOperations.Result.NO_OP));

        operations.playHistory().subscribe(trackSubscriber);

        trackSubscriber.assertValue(TRACK_ITEMS);
        trackSubscriber.assertCompleted();
    }

    @Test
    public void shouldForceSyncPlayHistoryOnRefresh() {
        when(syncOperations.sync(Syncable.PLAY_HISTORY))
                .thenReturn(Observable.just(SyncOperations.Result.NO_OP));

        operations.refreshPlayHistory().subscribe(trackSubscriber);

        trackSubscriber.assertValue(TRACK_ITEMS);
        trackSubscriber.assertCompleted();
    }

    @Test
    public void shouldReturnRecentlyPlayed() {
        operations.recentlyPlayed().subscribe(contextsSubscriber);

        contextsSubscriber.assertValues(RECENTLY_PLAYED_ITEM);
        contextsSubscriber.assertCompleted();
    }
}
