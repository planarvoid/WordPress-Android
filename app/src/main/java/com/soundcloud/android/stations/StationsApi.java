package com.soundcloud.android.stations;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.model.Urn;
import rx.Observable;

import javax.inject.Inject;

class StationsApi {
    private final ApiClientRx apiClientRx;

    @Inject
    public StationsApi(ApiClientRx apiClientRx) {
        this.apiClientRx = apiClientRx;
    }

    Observable<ApiStation> fetchStation(Urn stationUrn) {
        final ApiRequest request = ApiRequest
                .get(ApiEndpoints.STATION.path(stationUrn.toString()))
                .forPrivateApi(1)
                .build();

        return apiClientRx.mappedResponse(request, ApiStation.class);
    }
}
