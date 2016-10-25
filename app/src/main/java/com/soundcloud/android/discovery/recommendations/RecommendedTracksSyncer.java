package com.soundcloud.android.discovery.recommendations;


import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.java.reflect.TypeToken;
import com.soundcloud.propeller.WriteResult;

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.Callable;

class RecommendedTracksSyncer implements Callable<Boolean> {

    private final ApiClient apiClient;
    private final StoreRecommendationsCommand storeRecommendationsCommand;

    @Inject
    public RecommendedTracksSyncer(ApiClient apiClient, StoreRecommendationsCommand storeRecommendationsCommand) {
        this.apiClient = apiClient;
        this.storeRecommendationsCommand = storeRecommendationsCommand;
    }

    @Override
    public Boolean call() throws Exception {
        final ApiRequest request =
                ApiRequest.get(ApiEndpoints.TRACK_RECOMMENDATIONS.path())
                          .forPrivateApi()
                          .build();

        final ModelCollection<ApiRecommendation> apiRecommendations = getApiRecommendations(request);
        final WriteResult writeResult = storeRecommendationsCommand.call(apiRecommendations);
        return writeResult.success();
    }

    private ModelCollection<ApiRecommendation> getApiRecommendations(ApiRequest request)
            throws IOException, ApiRequestException, ApiMapperException {
        return apiClient.fetchMappedResponse(request, new TypeToken<ModelCollection<ApiRecommendation>>() {
        });
    }
}
