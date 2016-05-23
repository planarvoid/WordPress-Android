package com.soundcloud.android.stations;

import static com.soundcloud.android.stations.StationsCollectionsTypes.RECENT;
import static com.soundcloud.android.stations.StationsCollectionsTypes.RECOMMENDATIONS;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static com.soundcloud.java.collections.Lists.transform;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.discovery.DiscoveryItem;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.functions.Function;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.Scheduler;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RecommendedStationsOperationsTest extends AndroidUnitTest {
    private static final Urn URN_1 = Urn.forArtistStation(1);
    private static final Urn URN_2 = Urn.forTrackStation(2);
    private static final Urn URN_3 = Urn.forArtistStation(3);

    private static final StationRecord SUGGESTED_1 = StationFixtures.getStation(URN_1);
    private static final StationRecord SUGGESTED_2 = StationFixtures.getStation(URN_2);
    private static final StationRecord RECENT_1 = StationFixtures.getStation(Urn.forTrackStation(1001));
    private static final StationRecord RECENT_2 = StationFixtures.getStation(Urn.forTrackStation(1002));

    private static final ModelCollection<ApiStationMetadata> API_STATION_METADATA =
            StationFixtures.createStationsCollection(Arrays.asList(URN_1, URN_2, URN_3));

    private static final Function<ApiStationMetadata, StationRecord> TO_STATION_RECORD = new Function<ApiStationMetadata, StationRecord>() {
        @Override
        public StationRecord apply(ApiStationMetadata input) {
            return Station.from(input);
        }
    };

    private static final ArrayList<StationRecord> STATION_RECORDS =
            newArrayList(transform(API_STATION_METADATA.getCollection(), TO_STATION_RECORD));

    @Mock StationsStorage stationsStorage;
    @Mock StationsApi stationsApi;
    @Mock WriteStationsRecommendationsCommand writeCommand;
    @Mock FeatureFlags featureFlags;

    private RecommendedStationsOperations operations;
    private Scheduler scheduler = Schedulers.immediate();
    private TestSubscriber<DiscoveryItem> subscriber = new TestSubscriber<>();

    @Before
    public void setUp() throws Exception {
        when(featureFlags.isEnabled(Flag.RECOMMENDED_STATIONS)).thenReturn(true);
        when(stationsStorage.getStationsCollection(RECOMMENDATIONS))
                .thenReturn(Observable.<StationRecord>empty());
        when(stationsStorage.getStationsCollection(RECENT))
                .thenReturn(Observable.just(RECENT_1, RECENT_2));

        operations = new RecommendedStationsOperations(stationsStorage,
                stationsApi, writeCommand, featureFlags, scheduler);
    }

    @Test
    public void shouldReturnEmptyWhenFeatureIsDisabled() throws Exception {
        when(featureFlags.isEnabled(Flag.RECOMMENDED_STATIONS)).thenReturn(false);

        assertThat(operations.stationsBucket()).isEqualTo(Observable.empty());
    }

    @Test
    public void shouldReturnStoredRecommendations() throws Exception {
        when(stationsStorage.getStationsCollection(RECOMMENDATIONS))
                .thenReturn(Observable.just(SUGGESTED_1, SUGGESTED_2));

        operations.stationsBucket().subscribe(subscriber);

        assertThat(getStations()).containsExactly(SUGGESTED_1, SUGGESTED_2);
        subscriber.assertCompleted();
    }

    @Test
    public void shouldLoadNewRecommendationsWhenNoStoredRecommendations() throws Exception {
        when(stationsApi.fetchStationRecommendations())
                .thenReturn(Observable.just(API_STATION_METADATA));

        operations.stationsBucket().subscribe(subscriber);

        assertThat(getStations()).containsAll(STATION_RECORDS);
        subscriber.assertCompleted();
    }

    @Test
    public void shouldStoreNewRecommendations() throws Exception {
        when(stationsApi.fetchStationRecommendations())
                .thenReturn(Observable.just(API_STATION_METADATA));
        when(stationsStorage.getStationsCollection(RECOMMENDATIONS))
                .thenReturn(Observable.<StationRecord>empty());

        operations.stationsBucket().subscribe(subscriber);

        verify(writeCommand).call(API_STATION_METADATA);
    }

    @Test
    public void shouldReorderRecentStations() throws Exception {
        when(stationsStorage.getStationsCollection(RECOMMENDATIONS))
                .thenReturn(Observable.just(SUGGESTED_1, RECENT_1, SUGGESTED_2, RECENT_2));

        operations.stationsBucket().subscribe(subscriber);

        assertThat(getStations()).containsExactly(SUGGESTED_1, SUGGESTED_2, RECENT_2, RECENT_1);
    }

    @Test
    public void shouldNotReorderWhenNoRecentStations() throws Exception {
        when(stationsStorage.getStationsCollection(RECOMMENDATIONS))
                .thenReturn(Observable.just(SUGGESTED_1, RECENT_1, SUGGESTED_2, RECENT_2));
        when(stationsStorage.getStationsCollection(RECENT))
                .thenReturn(Observable.<StationRecord>empty());

        operations.stationsBucket().subscribe(subscriber);

        assertThat(getStations()).containsExactly(SUGGESTED_1, RECENT_1, SUGGESTED_2, RECENT_2);
    }

    @Test
    public void shouldNotAddRecentWhenNotSuggested() throws Exception {
        when(stationsStorage.getStationsCollection(RECOMMENDATIONS))
                .thenReturn(Observable.just(SUGGESTED_1, RECENT_1));
        when(stationsStorage.getStationsCollection(RECENT))
                .thenReturn(Observable.just(RECENT_2));

        operations.stationsBucket().subscribe(subscriber);

        assertThat(getStations()).containsExactly(SUGGESTED_1, RECENT_1);
    }

    @Test
    public void clearData() throws Exception {
        operations.clearData();

        verify(stationsStorage).clear();
    }

    private List<StationRecord> getStations() {
        DiscoveryItem discoveryItem = subscriber.getOnNextEvents().get(0);
        return ((RecommendedStationsBucket) discoveryItem).stationRecords;
    }

}
