package com.soundcloud.android.sync.likes;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.Command;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.utils.CollectionUtils;
import com.soundcloud.propeller.PropertySet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

abstract class PushLikesCommand<ApiModel extends PropertySetSource>
        extends Command<Collection<PropertySet>, Collection<PropertySet>, PushLikesCommand<ApiModel>> {

    private final ApiClient apiClient;
    private final ApiEndpoints endpoint;

    PushLikesCommand(ApiClient apiClient, ApiEndpoints endpoint) {
        this.apiClient = apiClient;
        this.endpoint = endpoint;
    }

    @Override
    public Collection<PropertySet> call() throws Exception {
        final List<Map<String, String>> urns = new ArrayList<>(input.size());
        for (PropertySet like : input) {
            urns.add(Collections.singletonMap("target_urn", like.get(LikeProperty.TARGET_URN).toString()));
        }
        final Map<String, List<Map<String, String>>> body = Collections.singletonMap("likes", urns);

        final ApiRequest.Builder<ModelCollection<ApiModel>> builder = requestBuilder(endpoint);
        final ApiRequest<ModelCollection<ApiModel>> request = builder.forPrivateApi(1).withContent(body).build();
        final ModelCollection<ApiModel> successSet = apiClient.fetchMappedResponse(request);
        return CollectionUtils.toPropertySets(successSet.getCollection());
    }

    protected abstract ApiRequest.Builder<ModelCollection<ApiModel>> requestBuilder(ApiEndpoints endpoint);
}
