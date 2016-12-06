package com.soundcloud.android.stations;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.configuration.experiments.SuggestedStationsExperiment;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.io.IOException;
import java.util.Collections;

public class StationsApiTest extends AndroidUnitTest {
    @Mock ApiClientRx apiClientRx;
    @Mock ApiClient apiClient;
    @Mock SuggestedStationsExperiment stationsExperiment;

    private final Urn stationUrn = Urn.forTrackStation(123L);
    private StationsApi api;
    private ApiStation apiStation;

    @Before
    public void setUp() {
        api = new StationsApi(apiClientRx, apiClient, stationsExperiment);

        apiStation = StationFixtures.getApiStation();
    }

    @Test
    public void shouldReturnAnApiStation() {
        final TestSubscriber<ApiStation> subscriber = new TestSubscriber<>();
        when(stationsExperiment.getVariantName()).thenReturn(Optional.<String>absent());
        when(apiClientRx.mappedResponse(argThat(isApiRequestTo("GET",
                                                               ApiEndpoints.STATION.path(stationUrn.toString()))),
                                        eq(ApiStation.class)))
                .thenReturn(Observable.just(apiStation));
        api.fetchStation(stationUrn).subscribe(subscriber);

        subscriber.assertReceivedOnNext(singletonList(apiStation));
    }

    @Test
    public void shouldSendExperimentWhenPresent() throws ApiRequestException, IOException, ApiMapperException {
        when(stationsExperiment.getVariantName()).thenReturn(Optional.of("variant_name"));
        final ApiStationMetadata station = new ApiStationMetadata(stationUrn, "", "", "", "");

        when(apiClient.fetchMappedResponse(argThat(
                isApiRequestTo("GET", ApiEndpoints
                        .STATION_RECOMMENDATIONS
                        .path(stationUrn.toString()))
                        .withQueryParam("variant", "variant_name")
        ), isA(TypeToken.class))).thenReturn(new ModelCollection<>(singletonList(station)));

        assertThat(api.fetchStationRecommendations().getCollection()).containsExactly(station);
    }

    @Test
    public void shouldReturnTrueWhenMigrationRequestWasSuccessful() {
        final TestSubscriber<Boolean> subscriber = new TestSubscriber<>();
        when(apiClientRx.response(argThat(isApiRequestTo("PUT", ApiEndpoints.STATIONS_MIGRATE_RECENT_TO_LIKED.path()))))
                .thenReturn(Observable.just(new ApiResponse(null, 201, "")));

        api.requestRecentToLikedMigration().subscribe(subscriber);
        subscriber.assertReceivedOnNext(Collections.singletonList(true));
    }

    @Test
    public void shouldReturnFalseWhenMigrationRequestFailed() {
        final TestSubscriber<Boolean> subscriber = new TestSubscriber<>();
        when(apiClientRx.response(argThat(isApiRequestTo("PUT", ApiEndpoints.STATIONS_MIGRATE_RECENT_TO_LIKED.path()))))
                .thenReturn(Observable.just(new ApiResponse(null, 500, "")));

        api.requestRecentToLikedMigration().subscribe(subscriber);
        subscriber.assertReceivedOnNext(Collections.singletonList(false));
    }
}
