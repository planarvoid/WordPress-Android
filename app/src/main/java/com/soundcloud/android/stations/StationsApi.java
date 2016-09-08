package com.soundcloud.android.stations;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.configuration.experiments.StationsRecoAlgorithmExperiment;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.reflect.TypeToken;

import rx.Observable;

import javax.inject.Inject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class StationsApi {
    private final ApiClientRx apiClientRx;
    private final ApiClient apiClient;
    private final StationsRecoAlgorithmExperiment stationsExperiment;

    @Inject
    public StationsApi(ApiClientRx apiClientRx,
                       ApiClient apiClient,
                       StationsRecoAlgorithmExperiment stationsExperiment) {
        this.apiClientRx = apiClientRx;
        this.apiClient = apiClient;
        this.stationsExperiment = stationsExperiment;
    }

    public ModelCollection<Urn> updateLikedStations(LikedStationsPostBody likedStationsPostBody) throws ApiRequestException, IOException, ApiMapperException {
        final ApiRequest.Builder builder = ApiRequest.put(ApiEndpoints.STATIONS_LIKED.path());
        final Map<String, List<String>> body = new HashMap<>(2);
        body.put("liked", Urns.toString(likedStationsPostBody.likedStations()));
        body.put("unliked", Urns.toString(likedStationsPostBody.unlikedStations()));

        final ApiRequest request = builder
                .forPrivateApi()
                .withContent(body)
                .build();
        return apiClient.fetchMappedResponse(request, new TypeToken<ModelCollection<Urn>>() {
        });
    }

    ModelCollection<ApiStationMetadata> fetchStationRecommendations() throws ApiRequestException, IOException, ApiMapperException {
        final ApiRequest.Builder builder = ApiRequest.get(ApiEndpoints.STATION_RECOMMENDATIONS.path());
        final ApiRequest request = builder
                .forPrivateApi()
                .build();

        return apiClient.fetchMappedResponse(request, new TypeToken<ModelCollection<ApiStationMetadata>>() {
        });
    }

    Observable<ApiStation> fetchStation(Urn stationUrn) {
        final ApiRequest.Builder builder = ApiRequest.get(ApiEndpoints.STATION.path(stationUrn.toString()));
        final Optional<String> variant = stationsExperiment.getVariantName();
        if (variant.isPresent()) {
            builder.addQueryParam("variant", variant.get());
        }
        final ApiRequest request = builder
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
