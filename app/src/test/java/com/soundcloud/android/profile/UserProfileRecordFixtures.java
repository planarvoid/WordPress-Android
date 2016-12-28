package com.soundcloud.android.profile;

import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.apiTrackHolder;
import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.create;
import static java.util.Collections.singletonList;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiTrackPost;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ModelCollection;

import java.util.Collections;

class UserProfileRecordFixtures {
    private static ModelCollection<ApiPlayableSource> emptyPlayableSources() {
        return new ModelCollection<>(Collections.<ApiPlayableSource>emptyList());
    }

    private static ModelCollection<ApiTrackPost> emptyTrackPosts() {
        return new ModelCollection<>(Collections.<ApiTrackPost>emptyList());
    }

    private static ModelCollection<ApiPlaylistPost> emptyPlaylistPosts() {
        return new ModelCollection<>(Collections.<ApiPlaylistPost>emptyList());
    }

    static class Builder {
        private ApiUser user = create(ApiUser.class);
        private ModelCollection<ApiPlayableSource> spotlight = emptyPlayableSources();
        private ModelCollection<ApiTrackPost> tracks = emptyTrackPosts();
        private ModelCollection<ApiPlaylistPost> albums = emptyPlaylistPosts();
        private ModelCollection<ApiPlaylistPost> playlists = emptyPlaylistPosts();
        private ModelCollection<ApiPlayableSource> reposts = emptyPlayableSources();
        private ModelCollection<ApiPlayableSource> likes = emptyPlayableSources();

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

        Builder albums(ModelCollection<ApiPlaylistPost> albums) {
            this.albums = albums;
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

        Builder populateAllCollections() {
            spotlight = new ModelCollection<>(singletonList(apiTrackHolder()));
            tracks = new ModelCollection<>(singletonList(new ApiTrackPost(create(ApiTrack.class))));
            albums = new ModelCollection<>(singletonList(new ApiPlaylistPost(create(ApiPlaylist.class))));
            playlists = new ModelCollection<>(singletonList(new ApiPlaylistPost(create(ApiPlaylist.class))));
            reposts = new ModelCollection<>(singletonList(apiTrackHolder()));
            likes = new ModelCollection<>(singletonList(apiTrackHolder()));

            return this;
        }

        ApiUserProfile build() {
            return new ApiUserProfile(
                    user,
                    spotlight,
                    tracks,
                    albums,
                    playlists,
                    reposts,
                    likes
            );
        }
    }
}
