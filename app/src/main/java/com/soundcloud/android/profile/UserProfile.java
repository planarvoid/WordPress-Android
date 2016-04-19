package com.soundcloud.android.profile;

import static com.soundcloud.java.collections.Lists.newArrayList;
import static com.soundcloud.java.collections.MoreCollections.filter;
import static com.soundcloud.java.collections.MoreCollections.transform;

import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.ApiEntityHolder;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.functions.Predicate;

import java.util.ArrayList;
import java.util.Collection;

public class UserProfile {
    private final PropertySet user;
    private final ModelCollection<PropertySet> spotlight;
    private final ModelCollection<PropertySet> tracks;
    private final ModelCollection<PropertySet> albums;
    private final ModelCollection<PropertySet> playlists;
    private final ModelCollection<PropertySet> reposts;
    private final ModelCollection<PropertySet> likes;

    public UserProfile(PropertySet user,
                       ModelCollection<PropertySet> spotlight,
                       ModelCollection<PropertySet> tracks,
                       ModelCollection<PropertySet> albums,
                       ModelCollection<PropertySet> playlists,
                       ModelCollection<PropertySet> reposts,
                       ModelCollection<PropertySet> likes) {
        this.user = user;
        this.spotlight = spotlight;
        this.tracks = tracks;
        this.albums = albums;
        this.playlists = playlists;
        this.reposts = reposts;
        this.likes = likes;
    }

    public PropertySet getUser() {
        return user;
    }

    public ModelCollection<PropertySet> getSpotlight() {
        return spotlight;
    }

    public ModelCollection<PropertySet> getTracks() {
        return tracks;
    }

    public ModelCollection<PropertySet> getAlbums() {
        return albums;
    }

    public ModelCollection<PropertySet> getPlaylists() {
        return playlists;
    }

    public ModelCollection<PropertySet> getReposts() {
        return reposts;
    }

    public ModelCollection<PropertySet> getLikes() {
        return likes;
    }

    public static UserProfile fromUserProfileRecord(UserProfileRecord userProfileRecord) {
        PropertySet user = userProfileRecord.getUser().toPropertySet();

        ModelCollection<PropertySet> spotlight = fromApiEntitySourceModelCollection(userProfileRecord.getSpotlight());
        ModelCollection<PropertySet> tracks = fromApiEntityModelCollection(userProfileRecord.getTracks());
        ModelCollection<PropertySet> albums = fromApiEntityModelCollection(userProfileRecord.getAlbums());
        ModelCollection<PropertySet> playlists = fromApiEntityModelCollection(userProfileRecord.getPlaylists());
        ModelCollection<PropertySet> reposts = fromApiEntitySourceModelCollection(userProfileRecord.getReposts());
        ModelCollection<PropertySet> likes = fromApiEntitySourceModelCollection(userProfileRecord.getLikes());

        return new UserProfile(user, spotlight, tracks, albums, playlists, reposts, likes);
    }

    private static ModelCollection<PropertySet> fromApiEntitySourceModelCollection(
            ModelCollection<? extends ApiEntityHolderSource> originalModelCollection) {
        Collection<? extends ApiEntityHolderSource> filteredSources = filter(originalModelCollection.getCollection(),
                new Predicate<ApiEntityHolderSource>() {
                    @Override
                    public boolean apply(ApiEntityHolderSource input) {
                        return input.getEntityHolder().isPresent();
                    }
                });

        Collection<ApiEntityHolder> apiEntityHolders = transform(filteredSources, new Function<ApiEntityHolderSource, ApiEntityHolder>() {
            @Override
            public ApiEntityHolder apply(ApiEntityHolderSource input) {
                return input.getEntityHolder().get();
            }
        });

        return new ModelCollection<>(
                propertySetsFromApiEntityHolders(apiEntityHolders),
                originalModelCollection.getLinks());
    }

    private static ModelCollection<PropertySet> fromApiEntityModelCollection(
            ModelCollection<? extends ApiEntityHolder> originalModelCollection) {
        return new ModelCollection<>(
                propertySetsFromApiEntityHolders(originalModelCollection.getCollection()),
                originalModelCollection.getLinks());
    }

    private static ArrayList<PropertySet> propertySetsFromApiEntityHolders(
            Collection<? extends ApiEntityHolder> apiEntityHolders) {
        return newArrayList(transform(apiEntityHolders, new Function<ApiEntityHolder, PropertySet>() {
            @Override
            public PropertySet apply(ApiEntityHolder input) {
                return input.toPropertySet();
            }
        }));
    }

}
