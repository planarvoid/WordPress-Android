package com.soundcloud.android.stations;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.configuration.experiments.StationsRecoAlgorithmExperiment;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;

public class StationsApiTest extends AndroidUnitTest {
    @Mock ApiClientRx apiClientRx;
    @Mock ApiClient apiClient;
    @Mock StationsRecoAlgorithmExperiment stationsExperiment;

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
    public void shouldSendExperimentWhenPresent() {
        final TestSubscriber<ApiStation> subscriber = new TestSubscriber<>();
        when(stationsExperiment.getVariantName()).thenReturn(Optional.of("variant_name"));
        when(apiClientRx.mappedResponse(argThat(
                isApiRequestTo("GET", ApiEndpoints.STATION.path(stationUrn.toString())).withQueryParam("variant",
                                                                                                       "variant_name")
        ), eq(ApiStation.class))).thenReturn(Observable.just(apiStation));

        api.fetchStation(stationUrn).subscribe(subscriber);

        subscriber.assertReceivedOnNext(singletonList(apiStation));
    }
}
