package com.soundcloud.android.sync.likes;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ModelCollection;

class PushLikeAdditionsCommand extends PushLikesCommand<ApiLike> {

    PushLikeAdditionsCommand(ApiClient apiClient, ApiEndpoints endpoint) {
        super(apiClient, endpoint);
    }

    @Override
    protected ApiRequest.Builder<ModelCollection<ApiLike>> requestBuilder(ApiEndpoints endpoint) {
        final TypeToken<ModelCollection<ApiLike>> typeToken = new TypeToken<ModelCollection<ApiLike>>() {
        };
        return ApiRequest.Builder.<ModelCollection<ApiLike>>post(endpoint.path()).forResource(typeToken);
    }
}
