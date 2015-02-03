package com.soundcloud.android.sync.likes;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ModelCollection;

class PushLikeDeletionsCommand extends PushLikesCommand<ApiDeletedLike> {

    PushLikeDeletionsCommand(ApiClient apiClient, ApiEndpoints endpoint) {
        super(apiClient, endpoint);
    }

    @Override
    protected ApiRequest.Builder<ModelCollection<ApiDeletedLike>> requestBuilder(ApiEndpoints endpoint) {
        final TypeToken<ModelCollection<ApiDeletedLike>> typeToken = new TypeToken<ModelCollection<ApiDeletedLike>>() {
        };
        return ApiRequest.Builder.<ModelCollection<ApiDeletedLike>>delete(endpoint.path()).forResource(typeToken);
    }
}
