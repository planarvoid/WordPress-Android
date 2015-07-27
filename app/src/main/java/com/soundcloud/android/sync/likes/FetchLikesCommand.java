package com.soundcloud.android.sync.likes;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.LegacyCommand;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.utils.PropertySetComparator;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.reflect.TypeToken;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.NavigableSet;
import java.util.TreeSet;

class FetchLikesCommand extends LegacyCommand<ApiEndpoints, NavigableSet<PropertySet>, FetchLikesCommand> {

    static final Comparator<PropertySet> LIKES_COMPARATOR = new PropertySetComparator<>(LikeProperty.TARGET_URN);

    private final ApiClient apiClient;

    @Inject
    FetchLikesCommand(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public NavigableSet<PropertySet> call() throws Exception {
        final ApiRequest request =
                ApiRequest.get(input.path())
                        .forPrivateApi(1)
                        .build();

        final ModelCollection<ApiLike> apiLikes = apiClient.fetchMappedResponse(request, new TypeToken<ModelCollection<ApiLike>>() {
        });
        final NavigableSet<PropertySet> result = new TreeSet<>(LIKES_COMPARATOR);
        for (ApiLike like : apiLikes) {
            result.add(like.toPropertySet());
        }
        return result;
    }
}
