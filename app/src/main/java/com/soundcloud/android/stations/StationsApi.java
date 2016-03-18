package com.soundcloud.android.stations;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;
import rx.Observable;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

class StationsApi {
    private final ApiClientRx apiClientRx;
    private final ApiClient apiClient;

    @Inject
    public StationsApi(ApiClientRx apiClientRx, ApiClient apiClient) {
        this.apiClientRx = apiClientRx;
        this.apiClient = apiClient;
    }

    Observable<ApiStation> fetchStation(Urn stationUrn) {
        final ApiRequest request = ApiRequest
                .get(ApiEndpoints.STATION.path(stationUrn.toString()))
                .forPrivateApi()
                .build();

        return apiClientRx.mappedResponse(request, ApiStation.class);
    }

    ApiStationsCollections syncStationsCollections(List<PropertySet> recentStationsToSync) throws ApiRequestException, IOException, ApiMapperException {
        final ApiRequest request = ApiRequest
                .post(ApiEndpoints.STATIONS.path())
                .withContent(new StationsSyncPostBody(recentStationsToSync))
                .forPrivateApi()
                .build();

        return apiClient.fetchMappedResponse(request, ApiStationsCollections.class);
    }
}
