package com.soundcloud.android.profile;

import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.api.model.ApiTrackPost;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;

class UserProfileFixtures {
    static class Builder {
        private ApiUser user = ModelFixtures.create(ApiUser.class);
        private ModelCollection<ApiPlayableSource> spotlight = null;
        private ModelCollection<ApiTrackPost> tracks = null;
        private ModelCollection<ApiPlaylistPost> releases = null;
        private ModelCollection<ApiPlaylistPost> playlists = null;
        private ModelCollection<ApiPlayableSource> reposts = null;
        private ModelCollection<ApiPlayableSource> likes = null;

        Builder user(ApiUser user) {
            this.user = user;
            return this;
        }

        Builder spotlight(ModelCollection<ApiPlayableSource> spotlight) {
            this.spotlight = spotlight;
            return this;
        }

        Builder tracks(ModelCollection<ApiTrackPost> tracks) {
            this.tracks = tracks;
            return this;
        }

        Builder releases(ModelCollection<ApiPlaylistPost> releases) {
            this.releases = releases;
            return this;
        }

        Builder playlists(ModelCollection<ApiPlaylistPost> playlists) {
            this.playlists = playlists;
            return this;
        }

        Builder reposts(ModelCollection<ApiPlayableSource> reposts) {
            this.reposts = reposts;
            return this;
        }

        Builder likes(ModelCollection<ApiPlayableSource> likes) {
            this.likes = likes;
            return this;
        }

        ApiUserProfile build() {
            return ApiUserProfile.create(
                    user,
                    spotlight,
                    tracks,
                    releases,
                    playlists,
                    reposts,
                    likes
            );
        }
    }
}
