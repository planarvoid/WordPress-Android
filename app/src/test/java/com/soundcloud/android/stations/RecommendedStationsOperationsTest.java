package com.soundcloud.android.stations;

import static com.soundcloud.android.stations.RecommendedStationsOperations.STATIONS_IN_BUCKET;
import static com.soundcloud.android.stations.StationsCollectionsTypes.RECENT;
import static com.soundcloud.android.stations.StationsCollectionsTypes.RECOMMENDATIONS;
import static com.soundcloud.java.collections.Lists.transform;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.just;

import com.soundcloud.android.discovery.DiscoveryItem;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.sync.SyncOperations;
import com.soundcloud.android.sync.SyncOperations.Result;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.functions.Function;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.Scheduler;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import java.util.ArrayList;
import java.util.List;

public class RecommendedStationsOperationsTest extends AndroidUnitTest {

    private static Function<StationRecord, StationViewModel> TO_VIEW_MODEL =
            new Function<StationRecord, StationViewModel>() {
                @Override
                public StationViewModel apply(StationRecord input) {
                    return viewModelFrom(input, false);
                }
            };

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
    @Mock PlayQueueManager playQueueManager;

    private RecommendedStationsOperations operations;
    private Scheduler scheduler = Schedulers.immediate();
    private TestSubscriber<DiscoveryItem> subscriber = new TestSubscriber<>();

    @Before
    public void setUp() throws Exception {
        when(stationsStorage.getStationsCollection(RECOMMENDATIONS)).thenReturn(Observable.<StationRecord>empty());
        when(stationsStorage.getStationsCollection(RECENT)).thenReturn(just(RECENT_1, RECENT_2));
        when(syncOperations.lazySyncIfStale(Syncable.RECOMMENDED_STATIONS)).thenReturn(syncSubject);
        when(syncStateStorage.hasSyncedBefore(Syncable.RECOMMENDED_STATIONS)).thenReturn(true);
        when(playQueueManager.getCollectionUrn()).thenReturn(Urn.NOT_SET);

        operations = new RecommendedStationsOperations(stationsStorage, playQueueManager, scheduler, syncOperations);
    }

    @Test
    public void shouldReturnStoredRecommendations() {
        when(stationsStorage.getStationsCollection(RECOMMENDATIONS))
                .thenReturn(just(SUGGESTED_1, SUGGESTED_2));

        operations.recommendedStations().subscribe(subscriber);
        subscriber.assertNoValues();

        syncSubject.onNext(Result.SYNCING);

        assertThat(getEmittedStations()).containsExactly(viewModelFrom(SUGGESTED_1), viewModelFrom(SUGGESTED_2));
    }

    @Test
    public void shouldReorderRecentStations() {
        when(stationsStorage.getStationsCollection(RECOMMENDATIONS))
                .thenReturn(just(SUGGESTED_1, RECENT_1, SUGGESTED_2, RECENT_2));

        operations.recommendedStations().subscribe(subscriber);
        syncSubject.onNext(Result.SYNCING);

        assertThat(getEmittedStations()).containsExactly(viewModelFrom(SUGGESTED_1), viewModelFrom(SUGGESTED_2),
                                                         viewModelFrom(RECENT_2), viewModelFrom(RECENT_1));
    }

    @Test
    public void shouldNotReorderWhenNoRecentStations() {
        when(stationsStorage.getStationsCollection(RECOMMENDATIONS))
                .thenReturn(just(SUGGESTED_1, RECENT_1, SUGGESTED_2, RECENT_2));
        when(stationsStorage.getStationsCollection(RECENT))
                .thenReturn(Observable.<StationRecord>empty());

        operations.recommendedStations().subscribe(subscriber);
        syncSubject.onNext(Result.SYNCING);

        assertThat(getEmittedStations()).containsExactly(viewModelFrom(SUGGESTED_1), viewModelFrom(RECENT_1),
                                                         viewModelFrom(SUGGESTED_2), viewModelFrom(RECENT_2));
    }

    @Test
    public void shouldNotAddRecentWhenNotSuggested() {
        when(stationsStorage.getStationsCollection(RECOMMENDATIONS))
                .thenReturn(just(SUGGESTED_1, RECENT_1));
        when(stationsStorage.getStationsCollection(RECENT))
                .thenReturn(just(RECENT_2));

        operations.recommendedStations().subscribe(subscriber);
        syncSubject.onNext(Result.SYNCING);

        assertThat(getEmittedStations()).containsExactly(viewModelFrom(SUGGESTED_1), viewModelFrom(RECENT_1));
    }

    @Test
    public void shouldSetCorrectNoPlayingStatus() {
        when(playQueueManager.getCollectionUrn()).thenReturn(SUGGESTED_1.getUrn());
        when(stationsStorage.getStationsCollection(RECOMMENDATIONS))
                .thenReturn(just(SUGGESTED_1, SUGGESTED_2));

        operations.recommendedStations().subscribe(subscriber);
        syncSubject.onNext(Result.SYNCING);

        assertThat(getEmittedStations()).containsExactly(viewModelFrom(SUGGESTED_1, true),
                                                         viewModelFrom(SUGGESTED_2, false));
    }

    @Test
    public void shouldAlwaysReturnGivenNumberOfSuggestedStations() {
        List<StationRecord> recommendedStations = stationRecordsList(STATIONS_IN_BUCKET + 2);
        List<StationRecord> recentlyPlayed = stationRecordsList(STATIONS_IN_BUCKET);
        when(stationsStorage.getStationsCollection(RECOMMENDATIONS)).thenReturn(Observable.from(recommendedStations));
        when(stationsStorage.getStationsCollection(RECENT)).thenReturn(Observable.from(recentlyPlayed));

        operations.recommendedStations().subscribe(subscriber);
        syncSubject.onNext(Result.SYNCING);

        List<StationViewModel> emitted = getEmittedStations();
        assertThat(emitted).containsAll(transform(recommendedStations.subList(0, STATIONS_IN_BUCKET), TO_VIEW_MODEL));
        assertThat(emitted).doesNotContainAnyElementsOf(transform(recentlyPlayed, TO_VIEW_MODEL));
    }

    @Test
    public void clearData() throws Exception {
        operations.clearData();

        verify(stationsStorage).clear();
    }

    private List<StationRecord> stationRecordsList(int count) {
        List<StationRecord> records = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            records.add(StationFixtures.getApiStation());
        }
        return records;
    }

    private List<StationViewModel> getEmittedStations() {
        DiscoveryItem discoveryItem = subscriber.getOnNextEvents().get(0);
        return ((RecommendedStationsBucketItem) discoveryItem).getStations();
    }

    private static StationViewModel viewModelFrom(StationRecord record, boolean isPlaying) {
        return new StationViewModel(record, isPlaying);
    }

    private static StationViewModel viewModelFrom(StationRecord record) {
        return new StationViewModel(record, false);
    }

}
