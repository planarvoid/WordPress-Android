package com.soundcloud.android.profile;

import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.api.model.ApiTrackPost;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;

import java.util.Collections;

class UserProfileFixtures {
    private static ModelCollection<ApiPlayableSource> getEmptySpotlight() {
        return new ModelCollection<>(
                Collections.<ApiPlayableSource>emptyList()
        );
    }

    private static ModelCollection<ApiTrackPost> getEmptyTracks() {
        return new ModelCollection<>(
                Collections.<ApiTrackPost>emptyList()
        );
    }

    private static ModelCollection<ApiPlaylistPost> getEmptyReleases() {
        return new ModelCollection<>(
                Collections.<ApiPlaylistPost>emptyList()
        );
    }

    private static ModelCollection<ApiPlaylistPost> getEmptyPlaylists() {
        return new ModelCollection<>(
                Collections.<ApiPlaylistPost>emptyList()
        );
    }

    private static ModelCollection<ApiPlayableSource> getEmptyReposts() {
        return new ModelCollection<>(
                Collections.<ApiPlayableSource>emptyList()
        );
    }

    private static ModelCollection<ApiPlayableSource> getEmptyLikes() {
        return new ModelCollection<>(
                Collections.<ApiPlayableSource>emptyList()
        );
    }

    static class Builder {
        private ApiUser user = ModelFixtures.create(ApiUser.class);
        private ModelCollection<ApiPlayableSource> spotlight = getEmptySpotlight();
        private ModelCollection<ApiTrackPost> tracks = getEmptyTracks();
        private ModelCollection<ApiPlaylistPost> releases = getEmptyReleases();
        private ModelCollection<ApiPlaylistPost> playlists = getEmptyPlaylists();
        private ModelCollection<ApiPlayableSource> reposts = getEmptyReposts();
        private ModelCollection<ApiPlayableSource> likes = getEmptyLikes();

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
