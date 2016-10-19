package com.soundcloud.android.collection.recentlyplayed;

import static java.util.Collections.singletonList;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.sync.SyncOperations;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.Scheduler;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.List;

public class RecentlyPlayedOperationsTest extends AndroidUnitTest {

    private static final List<RecentlyPlayedPlayableItem> CONTEXT_ITEMS = singletonList(mock(RecentlyPlayedPlayableItem.class));

    @Mock private RecentlyPlayedStorage recentlyPlayedStorage;
    @Mock private SyncOperations syncOperations;
    @Mock private ClearRecentlyPlayedCommand clearRecentlyPlayedCommand;

    private Scheduler scheduler = Schedulers.immediate();
    private TestSubscriber<List<RecentlyPlayedPlayableItem>> contextsSubscriber;

    private RecentlyPlayedOperations operations;


    @Before
    public void setUp() throws Exception {
        when(recentlyPlayedStorage.loadContexts(anyInt())).thenReturn(Observable.just(CONTEXT_ITEMS));
        contextsSubscriber = new TestSubscriber<>();
        operations = new RecentlyPlayedOperations(recentlyPlayedStorage, scheduler, syncOperations,
                                                  clearRecentlyPlayedCommand);
    }

    @Test
    public void shouldReturnRecentlyPlayed() throws Exception {
        when(syncOperations.lazySyncIfStale(Syncable.RECENTLY_PLAYED))
                .thenReturn(Observable.just(SyncOperations.Result.NO_OP));

        operations.recentlyPlayed().subscribe(contextsSubscriber);

        contextsSubscriber.assertValue(CONTEXT_ITEMS);
        contextsSubscriber.assertCompleted();
    }

    @Test
    public void shouldForceSyncRecentlyPlayedOnRefresh() {
        when(syncOperations.failSafeSync(Syncable.RECENTLY_PLAYED))
                .thenReturn(Observable.just(SyncOperations.Result.NO_OP));

        operations.refreshRecentlyPlayed().subscribe(contextsSubscriber);

        contextsSubscriber.assertValue(CONTEXT_ITEMS);
        contextsSubscriber.assertCompleted();
    }

}
