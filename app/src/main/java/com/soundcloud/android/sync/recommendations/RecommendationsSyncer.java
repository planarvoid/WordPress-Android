package com.soundcloud.android.sync.recommendations;


import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.java.reflect.TypeToken;

import javax.inject.Inject;
import java.util.concurrent.Callable;

public class RecommendationsSyncer implements Callable<Boolean> {

    private final ApiClient apiClient;
    private final StoreRecommendationsCommand storeRecommendationsCommand;

    @Inject
    public RecommendationsSyncer(ApiClient apiClient, StoreRecommendationsCommand storeRecommendationsCommand) {
        this.apiClient = apiClient;
        this.storeRecommendationsCommand = storeRecommendationsCommand;
    }

    @Override
    public Boolean call() throws Exception {
        final ApiRequest request =
                ApiRequest.get(ApiEndpoints.RECOMMENDATIONS.path())
                        .forPrivateApi(1)
                        .build();

        final ModelCollection<ApiRecommendation> apiRecommendations = getApiRecommendations(request);
        storeRecommendationsCommand.call(apiRecommendations);
        return true; // always assume something changed, because we always clear / write whatever we got
    }

    private ModelCollection<ApiRecommendation> getApiRecommendations(ApiRequest request)
            throws java.io.IOException, com.soundcloud.android.api.ApiRequestException, com.soundcloud.android.api.ApiMapperException {
        return apiClient.fetchMappedResponse(request, new TypeToken<ModelCollection<ApiRecommendation>>() {
        });
    }
}
