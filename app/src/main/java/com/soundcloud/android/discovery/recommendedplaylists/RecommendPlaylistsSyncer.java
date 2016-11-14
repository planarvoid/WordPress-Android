package com.soundcloud.android.discovery.recommendedplaylists;

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

public class RecommendPlaylistsSyncer implements Callable<Boolean> {
    private final ApiClient apiClient;
    private final StoreRecommendedPlaylistsCommand storeRecommendedPlaylistsCommand;

    @Inject
    RecommendPlaylistsSyncer(ApiClient apiClient, StoreRecommendedPlaylistsCommand storeRecommendedPlaylistsCommand) {
        this.apiClient = apiClient;
        this.storeRecommendedPlaylistsCommand = storeRecommendedPlaylistsCommand;
    }

    @Override
    public Boolean call() throws Exception {
        final ModelCollection<ApiRecommendedPlaylistBucket> apiRecommendedPlaylistBuckets = getApiRecommendedPlaylists();
        final WriteResult writeResult = storeRecommendedPlaylistsCommand.call(apiRecommendedPlaylistBuckets);
        return writeResult.success();
    }

    private ModelCollection<ApiRecommendedPlaylistBucket> getApiRecommendedPlaylists()
            throws IOException, ApiRequestException, ApiMapperException {
        ApiRequest request = ApiRequest.get(ApiEndpoints.RECOMMENDED_PLAYLISTS.path())
                                     .forPrivateApi()
                                     .build();

        return apiClient.fetchMappedResponse(request, new TypeToken<ModelCollection<ApiRecommendedPlaylistBucket>>() {
        });
    }

}
