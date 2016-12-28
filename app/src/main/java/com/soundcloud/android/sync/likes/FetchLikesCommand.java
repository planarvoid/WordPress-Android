package com.soundcloud.android.sync.likes;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.LegacyCommand;
import com.soundcloud.java.reflect.TypeToken;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.NavigableSet;
import java.util.TreeSet;

class FetchLikesCommand extends LegacyCommand<ApiEndpoints, NavigableSet<LikeRecord>, FetchLikesCommand> {

    public static final Comparator<LikeRecord> LIKES_COMPARATOR = new Comparator<LikeRecord>() {
        @Override
        public int compare(LikeRecord lhs, LikeRecord rhs) {
            return lhs.getTargetUrn().compareTo(rhs.getTargetUrn());
        }
    };

    private final ApiClient apiClient;

    @Inject
    FetchLikesCommand(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public NavigableSet<LikeRecord> call() throws Exception {
        final ApiRequest request =
                ApiRequest.get(input.path())
                          .forPrivateApi()
                          .build();

        final ModelCollection<ApiLike> apiLikes = apiClient.fetchMappedResponse(request,
                                                                                new TypeToken<ModelCollection<ApiLike>>() {
                                                                                });
        final NavigableSet<LikeRecord> result = new TreeSet<>(LIKES_COMPARATOR);
        for (ApiLike like : apiLikes) {
            result.add(like);
        }
        return result;
    }
}
