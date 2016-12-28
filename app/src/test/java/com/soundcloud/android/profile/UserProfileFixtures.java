package com.soundcloud.android.profile;

import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.create;
import static java.util.Collections.singletonList;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;

import java.util.Collections;

public class UserProfileFixtures {
    private static <T> ModelCollection<T> emptyModelCollection() {
        return new ModelCollection<>(Collections.emptyList());
    }

    static class Builder {
        private UserItem user = UserItem.from(create(ApiUser.class));
        private ModelCollection<PlayableItem> spotlight = emptyModelCollection();
        private ModelCollection<TrackItem> tracks = emptyModelCollection();
        private ModelCollection<PlaylistItem> albums = emptyModelCollection();
        private ModelCollection<PlaylistItem> playlists = emptyModelCollection();
        private ModelCollection<PlayableItem> reposts = emptyModelCollection();
        private ModelCollection<PlayableItem> likes = emptyModelCollection();

        Builder user(UserItem user) {
            this.user = user;
            return this;
        }

        Builder spotlight(ModelCollection<PlayableItem> spotlight) {
            this.spotlight = spotlight;
            return this;
        }

        Builder tracks(ModelCollection<TrackItem> tracks) {
            this.tracks = tracks;
            return this;
        }

        Builder albums(ModelCollection<PlaylistItem> albums) {
            this.albums = albums;
            return this;
        }

        Builder playlists(ModelCollection<PlaylistItem> playlists) {
            this.playlists = playlists;
            return this;
        }

        Builder reposts(ModelCollection<PlayableItem> reposts) {
            this.reposts = reposts;
            return this;
        }

        Builder likes(ModelCollection<PlayableItem> likes) {
            this.likes = likes;
            return this;
        }

        Builder populateAllCollections() {
            spotlight = new ModelCollection<>(singletonList(TrackItem.from(create(ApiTrack.class))));
            tracks = new ModelCollection<>(singletonList(TrackItem.from(create(ApiTrack.class))));
            albums = new ModelCollection<>(singletonList(PlaylistItem.from(create(ApiPlaylist.class))));
            playlists = new ModelCollection<>(singletonList(PlaylistItem.from(create(ApiPlaylist.class))));
            reposts = new ModelCollection<>(singletonList(TrackItem.from(create(ApiTrack.class))));
            likes = new ModelCollection<>(singletonList(TrackItem.from(create(ApiTrack.class))));

            return this;
        }

        UserProfile build() {
            return new UserProfile(user, spotlight, tracks, albums, playlists, reposts, likes);
        }
    }
}
