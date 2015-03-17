package com.soundcloud.android.sync.posts;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.LegacyCommand;
import com.soundcloud.propeller.PropertySet;

import javax.inject.Inject;
import java.util.NavigableSet;
import java.util.TreeSet;

class FetchPostsCommand extends LegacyCommand<ApiEndpoints, NavigableSet<PropertySet>, FetchPostsCommand> {

    private final ApiClient apiClient;

    @Inject
    FetchPostsCommand(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public NavigableSet<PropertySet> call() throws Exception {
        final ApiRequest<ModelCollection<ApiPostItem>> request =
                ApiRequest.Builder.<ModelCollection<ApiPostItem>>get(input.path())
                        .forPrivateApi(1)
                        .forResource(new TypeToken<ModelCollection<ApiPostItem>>() {})
                        .build();

        final ModelCollection<ApiPostItem> apiPosts = apiClient.fetchMappedResponse(request);
        final NavigableSet<PropertySet> result = new TreeSet<>(PostProperty.COMPARATOR);
        for (ApiPostItem post : apiPosts) {
            result.add(post.toPropertySet());
        }
        return result;
    }
}
