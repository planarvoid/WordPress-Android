package com.soundcloud.android.sync.posts;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.LegacyCommand;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.reflect.TypeToken;

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
        final ApiRequest request =
                ApiRequest.get(input.path())
                        .forPrivateApi(1)
                        .build();

        final ModelCollection<ApiPostItem> apiPosts = apiClient.fetchMappedResponse(request, new TypeToken<ModelCollection<ApiPostItem>>() {
        });
        final NavigableSet<PropertySet> result = new TreeSet<>(PostProperty.COMPARATOR);
        for (ApiPostItem post : apiPosts) {
            result.add(post.toPropertySet());
        }
        return result;
    }
}
