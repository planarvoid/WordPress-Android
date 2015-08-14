package com.soundcloud.android.stations;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;

import android.content.Context;

import java.util.Collections;

public class StationsApiTest extends AndroidUnitTest {
    @Mock Context context;
    @Mock ApiClientRx apiClientRx;

    private final Urn stationUrn = Urn.forTrackStation(123L);
    private StationsApi api;
    private ApiStation apiStation;

    @Before
    public void setUp() {
        api = new StationsApi(
                context,
                apiClientRx
        );

        apiStation = StationFixtures.getApiStation();
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(ApiStation.class))).thenReturn(Observable.just(apiStation));
    }

    @Test
    public void shouldReturnAnApiStation() {
        TestSubscriber<ApiStation> subscriber = new TestSubscriber<>();

        api.fetchStation(stationUrn).subscribe(subscriber);

        subscriber.assertReceivedOnNext(Collections.singletonList(apiStation));
    }
}