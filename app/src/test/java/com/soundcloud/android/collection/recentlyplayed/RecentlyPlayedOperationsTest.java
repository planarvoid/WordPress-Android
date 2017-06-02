package com.soundcloud.android.collection.recentlyplayed;

import static java.util.Collections.singletonList;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.sync.NewSyncOperations;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

public class RecentlyPlayedOperationsTest extends AndroidUnitTest {

    private static final List<RecentlyPlayedPlayableItem> CONTEXT_ITEMS = singletonList(mock(RecentlyPlayedPlayableItem.class));

    @Mock private RecentlyPlayedStorage recentlyPlayedStorage;
    @Mock private NewSyncOperations syncOperations;
    @Mock private ClearRecentlyPlayedCommand clearRecentlyPlayedCommand;

    private Scheduler scheduler = Schedulers.trampoline();

    private RecentlyPlayedOperations operations;


    @Before
    public void setUp() throws Exception {
        when(recentlyPlayedStorage.loadContexts(anyInt())).thenReturn(rx.Observable.just(CONTEXT_ITEMS));
        operations = new RecentlyPlayedOperations(recentlyPlayedStorage,
                                                  scheduler,
                                                  syncOperations,
                                                  clearRecentlyPlayedCommand);
    }

    @Test
    public void shouldReturnRecentlyPlayed() throws Exception {
        when(syncOperations.lazySyncIfStale(Syncable.RECENTLY_PLAYED))
                .thenReturn(Single.just(SyncResult.noOp()));

        operations.recentlyPlayed().test()
                  .assertValue(CONTEXT_ITEMS)
                  .assertComplete();
    }

    @Test
    public void shouldForceSyncRecentlyPlayedOnRefresh() {
        when(syncOperations.failSafeSync(Syncable.RECENTLY_PLAYED))
                .thenReturn(Single.just(SyncResult.noOp()));

        operations.refreshRecentlyPlayed().test()
                  .assertValue(CONTEXT_ITEMS)
                  .assertComplete();
    }

}
