package com.soundcloud.android.discovery.recommendedplaylists;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.optional.Optional;

import java.util.Collections;
import java.util.List;

public class RecommendedPlaylistsFixtures {

    static RecommendedPlaylistsEntity createEntity(List<Urn> urns) {
        return RecommendedPlaylistsEntity.create(123L, "chill", "Chill", Optional.absent(),
                                                 Optional.of(Urn.forDayZero(1234)), urns);
    }

    static RecommendedPlaylistsEntity createEmptyEntity() {
        return RecommendedPlaylistsEntity.create(123L, "chill", "Chill", Optional.absent(), Optional.of(Urn.forDayZero(1234)));
    }

    static ApiRecommendedPlaylistBucket createApiBucket() {
        ApiPlaylist apiPlaylist = ModelFixtures.create(ApiPlaylist.class);
        return ApiRecommendedPlaylistBucket.create("chill", "very chill", null, new ModelCollection<>(Collections.singletonList(apiPlaylist)));
    }
}