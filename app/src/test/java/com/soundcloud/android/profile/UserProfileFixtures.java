package com.soundcloud.android.profile;

import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.create;
import static java.util.Collections.singletonList;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.java.collections.PropertySet;

import java.util.Collections;

public class UserProfileFixtures {
    private static ModelCollection<PropertySet> emptyPropertySets() {
        return new ModelCollection<>(Collections.<PropertySet>emptyList());
    }

    static class Builder {
        private PropertySet user = create(ApiUser.class).toPropertySet();
        private ModelCollection<PropertySet> spotlight = emptyPropertySets();
        private ModelCollection<PropertySet> tracks = emptyPropertySets();
        private ModelCollection<PropertySet> albums = emptyPropertySets();
        private ModelCollection<PropertySet> playlists = emptyPropertySets();
        private ModelCollection<PropertySet> reposts = emptyPropertySets();
        private ModelCollection<PropertySet> likes = emptyPropertySets();

        Builder user(PropertySet user) {
            this.user = user;
            return this;
        }

        Builder spotlight(ModelCollection<PropertySet> spotlight) {
            this.spotlight = spotlight;
            return this;
        }

        Builder tracks(ModelCollection<PropertySet> tracks) {
            this.tracks = tracks;
            return this;
        }

        Builder albums(ModelCollection<PropertySet> albums) {
            this.albums = albums;
            return this;
        }

        Builder playlists(ModelCollection<PropertySet> playlists) {
            this.playlists = playlists;
            return this;
        }

        Builder reposts(ModelCollection<PropertySet> reposts) {
            this.reposts = reposts;
            return this;
        }

        Builder likes(ModelCollection<PropertySet> likes) {
            this.likes = likes;
            return this;
        }

        Builder populateAllCollections() {
            spotlight = new ModelCollection<>(singletonList(create(ApiTrack.class).toPropertySet()));
            tracks = new ModelCollection<>(singletonList(create(ApiTrack.class).toPropertySet()));
            albums = new ModelCollection<>(singletonList(create(ApiPlaylist.class).toPropertySet()));
            playlists = new ModelCollection<>(singletonList(create(ApiPlaylist.class).toPropertySet()));
            reposts = new ModelCollection<>(singletonList(create(ApiTrack.class).toPropertySet()));
            likes = new ModelCollection<>(singletonList(create(ApiTrack.class).toPropertySet()));

            return this;
        }

        UserProfile build() {
            return new UserProfile(user, spotlight, tracks, albums, playlists, reposts, likes);
        }
    }
}
