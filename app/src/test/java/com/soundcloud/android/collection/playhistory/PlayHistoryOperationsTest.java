package com.soundcloud.android.collection.playhistory;

import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.trackItem;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.sync.SyncOperations;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.Scheduler;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.List;

public class PlayHistoryOperationsTest extends AndroidUnitTest {

    private static final List<Track> TRACKS= ModelFixtures.tracks(10);

    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private PlayHistoryStorage playHistoryStorage;
    @Mock private SyncOperations syncOperations;
    @Mock private ClearPlayHistoryCommand clearCommand;

    private Scheduler scheduler = Schedulers.immediate();
    private TestSubscriber<List<TrackItem>> trackSubscriber;
    private TestSubscriber<Boolean> clearSubscriber;
    private List<TrackItem> trackItems;

    private PlayHistoryOperations operations;

    @Before
    public void setUp() throws Exception {
        when(playHistoryStorage.loadTracks(anyInt())).thenReturn(Observable.from(TRACKS));
        trackItems = new ArrayList<>(TRACKS.size());
        for (Track track : TRACKS) {
            trackItems.add(trackItem(track));
        }
        trackSubscriber = new TestSubscriber<>();
        clearSubscriber = new TestSubscriber<>();

        operations = new PlayHistoryOperations(playbackInitiator, playHistoryStorage, scheduler,
                                               syncOperations, clearCommand, ModelFixtures.trackItemCreator());
    }

    @Test
    public void shouldReturnPlayHistory() throws Exception {
        when(syncOperations.lazySyncIfStale(Syncable.PLAY_HISTORY))
                .thenReturn(Observable.just(SyncOperations.Result.NO_OP));

        operations.playHistory().subscribe(trackSubscriber);

        trackSubscriber.assertValue(trackItems);
        trackSubscriber.assertCompleted();
    }

    @Test
    public void shouldForceSyncPlayHistoryOnRefresh() {
        when(syncOperations.failSafeSync(Syncable.PLAY_HISTORY))
                .thenReturn(Observable.just(SyncOperations.Result.NO_OP));

        operations.refreshPlayHistory().subscribe(trackSubscriber);

        trackSubscriber.assertValue(trackItems);
        trackSubscriber.assertCompleted();
    }

    @Test
    public void shouldClearHistory() {
        when(clearCommand.toObservable(null)).thenReturn(Observable.just(true));

        operations.clearHistory().subscribe(clearSubscriber);

        clearSubscriber.assertValues(true);
    }
}
