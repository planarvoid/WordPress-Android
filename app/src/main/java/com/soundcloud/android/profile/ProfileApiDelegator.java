package com.soundcloud.android.profile;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import dagger.Lazy;
import rx.Observable;

import javax.inject.Inject;

public class ProfileApiDelegator implements ProfileApi {

    private final Lazy<ProfileApiPublic> profileApiPublic;
    private final Lazy<ProfileApiMobile> profileApiMobile;
    private final FeatureFlags featureFlags;

    @Inject
    public ProfileApiDelegator(Lazy<ProfileApiPublic> profileApiPublic,
                               Lazy<ProfileApiMobile> profileApiMobile,
                               FeatureFlags featureFlags) {
        this.profileApiPublic = profileApiPublic;
        this.profileApiMobile = profileApiMobile;
        this.featureFlags = featureFlags;
    }

    @Override
    public Observable<ModelCollection<PropertySetSource>> userPosts(Urn user) {
        return featureFlags.isEnabled(Flag.PROFILE_API_MOBILE)
                ? profileApiMobile.get().userPosts(user)
                : profileApiPublic.get().userPosts(user);
    }

    @Override
    public Observable<ModelCollection<PropertySetSource>> userPosts(String nextPageLink) {
        return featureFlags.isEnabled(Flag.PROFILE_API_MOBILE)
                ? profileApiMobile.get().userPosts(nextPageLink)
                : profileApiPublic.get().userPosts(nextPageLink);
    }

    @Override
    public Observable<ModelCollection<ApiPlaylist>> userPlaylists(Urn user) {
        return profileApiPublic.get().userPlaylists(user);
    }

    @Override
    public Observable<ModelCollection<ApiPlaylist>> userPlaylists(String nextPageLink) {
        return profileApiPublic.get().userPlaylists(nextPageLink);
    }

    @Override
    public Observable<ModelCollection<PropertySetSource>> userLikes(Urn user) {
        return featureFlags.isEnabled(Flag.PROFILE_API_MOBILE)
                ? profileApiMobile.get().userLikes(user)
                : profileApiPublic.get().userLikes(user);
    }

    @Override
    public Observable<ModelCollection<PropertySetSource>> userLikes(String nextPageLink) {
        return featureFlags.isEnabled(Flag.PROFILE_API_MOBILE)
                ? profileApiMobile.get().userLikes(nextPageLink)
                : profileApiPublic.get().userLikes(nextPageLink);
    }

    @Override
    public Observable<ModelCollection<ApiUser>> userFollowings(Urn user) {
        return profileApiPublic.get().userFollowings(user);
    }

    @Override
    public Observable<ModelCollection<ApiUser>> userFollowings(String nextPageLink) {
        return profileApiPublic.get().userFollowings(nextPageLink);
    }

    @Override
    public Observable<ModelCollection<ApiUser>> userFollowers(Urn user) {
        return profileApiPublic.get().userFollowers(user);
    }

    @Override
    public Observable<ModelCollection<ApiUser>> userFollowers(String nextPageLink) {
        return profileApiPublic.get().userFollowers(nextPageLink);
    }
}
