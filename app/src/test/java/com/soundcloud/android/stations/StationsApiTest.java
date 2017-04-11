package com.soundcloud.android.stations;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;

public class StationsApiTest extends AndroidUnitTest {
    @Mock ApiClientRx apiClientRx;
    @Mock ApiClient apiClient;

    private final Urn stationUrn = Urn.forTrackStation(123L);
    private StationsApi api;
    private ApiStation apiStation;

    @Before
    public void setUp() {
        api = new StationsApi(apiClientRx, apiClient);

        apiStation = StationFixtures.getApiStation();
    }

    @Test
    public void shouldReturnAnApiStation() {
        final TestSubscriber<ApiStation> subscriber = new TestSubscriber<>();
        when(apiClientRx.mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.STATION.path(stationUrn.toString()))),
                                        eq(ApiStation.class)))
                .thenReturn(Observable.just(apiStation));
        api.fetchStation(stationUrn).subscribe(subscriber);

        subscriber.assertReceivedOnNext(singletonList(apiStation));
    }

}
