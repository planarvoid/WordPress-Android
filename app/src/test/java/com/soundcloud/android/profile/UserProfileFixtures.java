package com.soundcloud.android.profile;

import static java.util.Collections.singletonList;

import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.testsupport.PlaylistFixtures;
import com.soundcloud.android.testsupport.TrackFixtures;
import com.soundcloud.android.testsupport.UserFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;

import java.util.Collections;

public class UserProfileFixtures {
    private static <T> ModelCollection<T> emptyModelCollection() {
        return new ModelCollection<>(Collections.emptyList());
    }

    static class Builder {
        private UserItem user = UserFixtures.userItem();
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
            spotlight = new ModelCollection<>(singletonList(TrackFixtures.trackItem()));
            tracks = new ModelCollection<>(singletonList(TrackFixtures.trackItem()));
            albums = new ModelCollection<>(singletonList(PlaylistFixtures.playlistItem(PlaylistFixtures.apiPlaylist())));
            playlists = new ModelCollection<>(singletonList(PlaylistFixtures.playlistItem(PlaylistFixtures.apiPlaylist())));
            reposts = new ModelCollection<>(singletonList(TrackFixtures.trackItem()));
            likes = new ModelCollection<>(singletonList(TrackFixtures.trackItem()));

            return this;
        }

        UserProfile build() {
            return new UserProfile(user, spotlight, tracks, albums, playlists, reposts, likes);
        }
    }
}
