package com.soundcloud.android.profile;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.ApiEntityHolder;
import com.soundcloud.android.model.Urn;
import dagger.Lazy;
import rx.Observable;

import javax.inject.Inject;

public class ProfileApiDelegator implements ProfileApi {

    private final Lazy<ProfileApiPublic> profileApiPublic;
    private final Lazy<ProfileApiMobile> profileApiMobile;

    @Inject
    public ProfileApiDelegator(Lazy<ProfileApiPublic> profileApiPublic,
                               Lazy<ProfileApiMobile> profileApiMobile) {
        this.profileApiPublic = profileApiPublic;
        this.profileApiMobile = profileApiMobile;
    }

    @Override
    public Observable<ModelCollection<ApiEntityHolder>> userPosts(Urn user) {
        return profileApiMobile.get().userPosts(user);
    }

    @Override
    public Observable<ModelCollection<ApiEntityHolder>> userPosts(String nextPageLink) {
        return profileApiMobile.get().userPosts(nextPageLink);
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
    public Observable<ModelCollection<ApiEntityHolder>> userLikes(Urn user) {
        return profileApiMobile.get().userLikes(user);
    }

    @Override
    public Observable<ModelCollection<ApiEntityHolder>> userLikes(String nextPageLink) {
        return profileApiMobile.get().userLikes(nextPageLink);
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

    @Override
    public Observable<ApiUserProfile> userProfile(Urn user) {
        return profileApiMobile.get().userProfile(user);
    }
}
