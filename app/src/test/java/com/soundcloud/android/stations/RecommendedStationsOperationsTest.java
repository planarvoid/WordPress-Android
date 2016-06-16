package com.soundcloud.android.stations;

import static com.soundcloud.android.stations.StationsCollectionsTypes.RECENT;
import static com.soundcloud.android.stations.StationsCollectionsTypes.RECOMMENDATIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.just;

import com.soundcloud.android.discovery.DiscoveryItem;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncOperations;
import com.soundcloud.android.sync.SyncOperations.Result;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.Scheduler;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import java.util.List;

public class RecommendedStationsOperationsTest extends AndroidUnitTest {
    private static final Urn URN_1 = Urn.forArtistStation(1);
    private static final Urn URN_2 = Urn.forTrackStation(2);

    private static final StationRecord SUGGESTED_1 = StationFixtures.getStation(URN_1);
    private static final StationRecord SUGGESTED_2 = StationFixtures.getStation(URN_2);
    private static final StationRecord RECENT_1 = StationFixtures.getStation(Urn.forTrackStation(1001));
    private static final StationRecord RECENT_2 = StationFixtures.getStation(Urn.forTrackStation(1002));

    private final PublishSubject<Result> syncSubject = PublishSubject.create();

    @Mock StationsStorage stationsStorage;
    @Mock SyncStateStorage syncStateStorage;
    @Mock SyncOperations syncOperations;

    private RecommendedStationsOperations operations;
    private Scheduler scheduler = Schedulers.immediate();
    private TestSubscriber<DiscoveryItem> subscriber = new TestSubscriber<>();

    @Before
    public void setUp() throws Exception {
        when(stationsStorage.getStationsCollection(RECOMMENDATIONS)).thenReturn(Observable.<StationRecord>empty());
        when(stationsStorage.getStationsCollection(RECENT)).thenReturn(just(RECENT_1, RECENT_2));
        when(syncOperations.lazySyncIfStale(Syncable.RECOMMENDED_STATIONS)).thenReturn(syncSubject);
        when(syncStateStorage.hasSyncedBefore(Syncable.RECOMMENDED_STATIONS)).thenReturn(true);

        operations = new RecommendedStationsOperations(stationsStorage, scheduler, syncOperations);
    }

    @Test
    public void shouldReturnStoredRecommendations() throws Exception {
        when(stationsStorage.getStationsCollection(RECOMMENDATIONS))
                .thenReturn(just(SUGGESTED_1, SUGGESTED_2));

        operations.stationsBucket().subscribe(subscriber);
        subscriber.assertNoValues();

        syncSubject.onNext(Result.SYNCING);

        assertThat(getStations()).containsExactly(SUGGESTED_1, SUGGESTED_2);
        subscriber.assertCompleted();
    }

    @Test
    public void shouldReorderRecentStations() throws Exception {
        when(stationsStorage.getStationsCollection(RECOMMENDATIONS))
                .thenReturn(just(SUGGESTED_1, RECENT_1, SUGGESTED_2, RECENT_2));

        operations.stationsBucket().subscribe(subscriber);
        syncSubject.onNext(Result.SYNCING);

        assertThat(getStations()).containsExactly(SUGGESTED_1, SUGGESTED_2, RECENT_2, RECENT_1);
    }

    @Test
    public void shouldNotReorderWhenNoRecentStations() throws Exception {
        when(stationsStorage.getStationsCollection(RECOMMENDATIONS))
                .thenReturn(just(SUGGESTED_1, RECENT_1, SUGGESTED_2, RECENT_2));
        when(stationsStorage.getStationsCollection(RECENT))
                .thenReturn(Observable.<StationRecord>empty());

        operations.stationsBucket().subscribe(subscriber);
        syncSubject.onNext(Result.SYNCING);

        assertThat(getStations()).containsExactly(SUGGESTED_1, RECENT_1, SUGGESTED_2, RECENT_2);
    }

    @Test
    public void shouldNotAddRecentWhenNotSuggested() throws Exception {
        when(stationsStorage.getStationsCollection(RECOMMENDATIONS))
                .thenReturn(just(SUGGESTED_1, RECENT_1));
        when(stationsStorage.getStationsCollection(RECENT))
                .thenReturn(just(RECENT_2));

        operations.stationsBucket().subscribe(subscriber);
        syncSubject.onNext(Result.SYNCING);

        assertThat(getStations()).containsExactly(SUGGESTED_1, RECENT_1);
    }

    @Test
    public void clearData() throws Exception {
        operations.clearData();

        verify(stationsStorage).clear();
    }

    private List<StationRecord> getStations() {
        DiscoveryItem discoveryItem = subscriber.getOnNextEvents().get(0);
        return ((RecommendedStationsItem) discoveryItem).getStationRecords();
    }

}
