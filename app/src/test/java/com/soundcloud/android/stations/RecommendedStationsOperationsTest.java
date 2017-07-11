package com.soundcloud.android.stations;

import static com.soundcloud.android.stations.RecommendedStationsOperations.STATIONS_IN_BUCKET;
import static com.soundcloud.android.stations.StationsCollectionsTypes.RECENT;
import static com.soundcloud.android.stations.StationsCollectionsTypes.RECOMMENDATIONS;
import static com.soundcloud.java.collections.Lists.transform;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.olddiscovery.OldDiscoveryItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.sync.NewSyncOperations;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class RecommendedStationsOperationsTest {

    private static Function<StationRecord, StationViewModel> TO_VIEW_MODEL =
            input -> viewModelFrom(input, false);

    private static final Urn URN_1 = Urn.forArtistStation(1);
    private static final Urn URN_2 = Urn.forTrackStation(2);

    private static final StationRecord SUGGESTED_1 = StationFixtures.getStation(URN_1);
    private static final StationRecord SUGGESTED_2 = StationFixtures.getStation(URN_2);
    private static final StationRecord RECENT_1 = StationFixtures.getStation(Urn.forTrackStation(1001));
    private static final StationRecord RECENT_2 = StationFixtures.getStation(Urn.forTrackStation(1002));

    PublishSubject<SyncResult> syncSubject;

    @Mock StationsStorage stationsStorage;
    @Mock SyncStateStorage syncStateStorage;
    @Mock NewSyncOperations syncOperations;
    @Mock PlayQueueManager playQueueManager;

    private RecommendedStationsOperations operations;
    private Scheduler scheduler = Schedulers.trampoline();

    @Before
    public void setUp() throws Exception {
        syncSubject = PublishSubject.create();
        when(stationsStorage.getStationsCollection(RECOMMENDATIONS)).thenReturn(Single.just(Lists.newArrayList()));
        when(stationsStorage.getStationsCollection(RECENT)).thenReturn(Single.just(Lists.newArrayList(RECENT_1, RECENT_2)));
        when(syncOperations.lazySyncIfStale(Syncable.RECOMMENDED_STATIONS)).thenReturn(syncSubject.firstOrError());
        when(playQueueManager.getCollectionUrn()).thenReturn(Urn.NOT_SET);

        operations = new RecommendedStationsOperations(stationsStorage, playQueueManager, scheduler, syncOperations);
    }

    @Test
    public void shouldReturnStoredRecommendations() {
        when(stationsStorage.getStationsCollection(RECOMMENDATIONS))
                .thenReturn(Single.just(Lists.newArrayList(SUGGESTED_1, SUGGESTED_2)));

        TestObserver<OldDiscoveryItem> subscriber = operations.recommendedStations().test();
        subscriber.assertNoValues();

        syncSubject.onNext(SyncResult.syncing());

        assertThat(getEmittedStations(subscriber)).containsExactly(viewModelFrom(SUGGESTED_1), viewModelFrom(SUGGESTED_2));
    }

    @Test
    public void shouldReorderRecentStations() {
        when(stationsStorage.getStationsCollection(RECOMMENDATIONS))
                .thenReturn(Single.just(Lists.newArrayList(SUGGESTED_1, RECENT_1, SUGGESTED_2, RECENT_2)));

        TestObserver<OldDiscoveryItem> subscriber = operations.recommendedStations().test();
        syncSubject.onNext(SyncResult.syncing());

        assertThat(getEmittedStations(subscriber)).containsExactly(viewModelFrom(SUGGESTED_1), viewModelFrom(SUGGESTED_2),
                                                         viewModelFrom(RECENT_2), viewModelFrom(RECENT_1));
    }

    @Test
    public void shouldNotReorderWhenNoRecentStations() {
        when(stationsStorage.getStationsCollection(RECOMMENDATIONS))
                .thenReturn(Single.just(Lists.newArrayList(SUGGESTED_1, RECENT_1, SUGGESTED_2, RECENT_2)));
        when(stationsStorage.getStationsCollection(RECENT))
                .thenReturn(Single.just(Lists.newArrayList()));

        TestObserver<OldDiscoveryItem> subscriber = operations.recommendedStations().test();
        syncSubject.onNext(SyncResult.syncing());

        assertThat(getEmittedStations(subscriber)).containsExactly(viewModelFrom(SUGGESTED_1), viewModelFrom(RECENT_1),
                                                         viewModelFrom(SUGGESTED_2), viewModelFrom(RECENT_2));
    }

    @Test
    public void shouldNotAddRecentWhenNotSuggested() {
        when(stationsStorage.getStationsCollection(RECOMMENDATIONS))
                .thenReturn(Single.just(Lists.newArrayList(SUGGESTED_1, RECENT_1)));
        when(stationsStorage.getStationsCollection(RECENT))
                .thenReturn(Single.just(Lists.newArrayList(RECENT_2)));

        TestObserver<OldDiscoveryItem> subscriber = operations.recommendedStations().test();
        syncSubject.onNext(SyncResult.syncing());

        assertThat(getEmittedStations(subscriber)).containsExactly(viewModelFrom(SUGGESTED_1), viewModelFrom(RECENT_1));
    }

    @Test
    public void shouldSetCorrectNoPlayingStatus() {
        when(playQueueManager.getCollectionUrn()).thenReturn(SUGGESTED_1.getUrn());
        when(stationsStorage.getStationsCollection(RECOMMENDATIONS))
                .thenReturn(Single.just(Lists.newArrayList(SUGGESTED_1, SUGGESTED_2)));

        TestObserver<OldDiscoveryItem> subscriber = operations.recommendedStations().test();
        syncSubject.onNext(SyncResult.syncing());

        assertThat(getEmittedStations(subscriber)).containsExactly(viewModelFrom(SUGGESTED_1, true),
                                                         viewModelFrom(SUGGESTED_2, false));
    }

    @Test
    public void shouldAlwaysReturnGivenNumberOfSuggestedStations() {
        List<StationRecord> recommendedStations = stationRecordsList(STATIONS_IN_BUCKET + 2);
        List<StationRecord> recentlyPlayed = stationRecordsList(STATIONS_IN_BUCKET);
        when(stationsStorage.getStationsCollection(RECOMMENDATIONS)).thenReturn(Single.just(Lists.newArrayList(recommendedStations)));
        when(stationsStorage.getStationsCollection(RECENT)).thenReturn(Single.just(Lists.newArrayList(recentlyPlayed)));

        TestObserver<OldDiscoveryItem> subscriber = operations.recommendedStations().test();
        syncSubject.onNext(SyncResult.syncing());

        List<StationViewModel> emitted = getEmittedStations(subscriber);
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

    private List<StationViewModel> getEmittedStations(TestObserver<OldDiscoveryItem> subscriber) {
        OldDiscoveryItem oldDiscoveryItem = subscriber.values().get(0);
        return ((RecommendedStationsBucketItem) oldDiscoveryItem).getStations();
    }

    private static StationViewModel viewModelFrom(StationRecord record, boolean isPlaying) {
        return StationViewModel.create(record, isPlaying);
    }

    private static StationViewModel viewModelFrom(StationRecord record) {
        return StationViewModel.create(record, false);
    }

}
