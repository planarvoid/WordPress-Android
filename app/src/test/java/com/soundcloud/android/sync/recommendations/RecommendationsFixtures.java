package com.soundcloud.android.sync.recommendations;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class RecommendationsFixtures {

    private static final String LIKED_RELATION_KEY = "liked";
    private static final String LISTENED_TO_RELATION_KEY = "listened_to";
    private static final String UNKNOWN_RELATION_KEY = "unknown";

    public static List<ApiRecommendation> createApiRecommendationsWithLikedReason(int count) {
        return createApiRecommendations(LIKED_RELATION_KEY, count);
    }

    public static List<ApiRecommendation> createApiRecommendationsWithListenedToReason(int count) {
        return createApiRecommendations(LISTENED_TO_RELATION_KEY, count);
    }

    public static List<ApiRecommendation> createApiRecommendationsWithUnknownReason(int count) {
        return createApiRecommendations(UNKNOWN_RELATION_KEY, count);
    }

    @NonNull
    private static List<ApiRecommendation> createApiRecommendations(String relationKey, int count) {
        List<ApiRecommendation> recommendations = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            recommendations.add(createApiRecommendation(relationKey));
        }
        return recommendations;
    }

    @NonNull
    private static ApiRecommendation createApiRecommendation(String relationKey) {
        return new ApiRecommendation(
                ModelFixtures.create(ApiTrack.class),
                relationKey,
                new ModelCollection<>(ModelFixtures.create(ApiTrack.class, 2))
        );
    }
}
