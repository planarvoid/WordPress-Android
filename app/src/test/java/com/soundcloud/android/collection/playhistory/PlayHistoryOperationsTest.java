package com.soundcloud.android.collection.playhistory;

import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.trackItem;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.sync.NewSyncOperations;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

public class PlayHistoryOperationsTest extends AndroidUnitTest {

    private static final List<Track> TRACKS = ModelFixtures.tracks(10);

    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private PlayHistoryStorage playHistoryStorage;
    @Mock private NewSyncOperations syncOperations;
    @Mock private ClearPlayHistoryCommand clearCommand;

    private Scheduler scheduler = io.reactivex.schedulers.Schedulers.trampoline();
    private List<TrackItem> trackItems;

    private PlayHistoryOperations operations;

    @Before
    public void setUp() throws Exception {
        when(playHistoryStorage.loadTracks(anyInt())).thenReturn(Observable.just(TRACKS));
        trackItems = new ArrayList<>(TRACKS.size());
        for (Track track : TRACKS) {
            trackItems.add(trackItem(track));
        }

        operations = new PlayHistoryOperations(playbackInitiator,
                                               playHistoryStorage,
                                               scheduler,
                                               syncOperations,
                                               clearCommand,
                                               ModelFixtures.entityItemCreator());
    }

    @Test
    public void shouldReturnPlayHistory() throws Exception {
        when(syncOperations.lazySyncIfStale(Syncable.PLAY_HISTORY))
                .thenReturn(Single.just(SyncResult.noOp()));

        operations.playHistory().test()
                  .assertValue(trackItems)
                  .assertComplete();
    }

    @Test
    public void shouldForceSyncPlayHistoryOnRefresh() {
        when(syncOperations.lazySyncIfStale(Syncable.PLAY_HISTORY))
                .thenReturn(Single.just(SyncResult.noOp()));

        operations.playHistory().test()
                  .assertValue(trackItems)
                  .assertComplete();
    }

    @Test
    public void shouldClearHistory() {
        when(clearCommand.toObservable(null)).thenReturn(rx.Observable.just(true));

        operations.clearHistory().test()
                  .assertValue(true);
    }
}
