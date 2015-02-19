package com.soundcloud.android.sync.likes;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.Command;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.utils.PropertySetComparator;
import com.soundcloud.propeller.PropertySet;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.NavigableSet;
import java.util.TreeSet;

class FetchLikesCommand extends Command<ApiEndpoints, NavigableSet<PropertySet>, FetchLikesCommand> {

    static final Comparator<PropertySet> LIKES_COMPARATOR = new PropertySetComparator<>(LikeProperty.TARGET_URN);

    private final ApiClient apiClient;

    @Inject
    FetchLikesCommand(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public NavigableSet<PropertySet> call() throws Exception {
        final ApiRequest<ModelCollection<ApiLike>> request =
                ApiRequest.Builder.<ModelCollection<ApiLike>>get(input.path())
                        .forPrivateApi(1)
                        .forResource(new TypeToken<ModelCollection<ApiLike>>() {})
                        .build();

        final ModelCollection<ApiLike> apiLikes = apiClient.fetchMappedResponse(request);
        final NavigableSet<PropertySet> result = new TreeSet<>(LIKES_COMPARATOR);
        for (ApiLike like : apiLikes) {
            result.add(like.toPropertySet());
        }
        return result;
    }
}
