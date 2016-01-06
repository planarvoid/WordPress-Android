package com.soundcloud.android.profile;

import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Banana;
import com.soundcloud.android.model.Urn;

import javax.inject.Inject;

import dagger.Lazy;
import rx.Observable;

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
    public Observable<ModelCollection<Banana>> userPosts(Urn user) {
        return profileApiMobile.get().userPosts(user);
    }

    @Override
    public Observable<ModelCollection<Banana>> userPosts(String nextPageLink) {
        return profileApiMobile.get().userPosts(nextPageLink);
    }

    @Override
    public Observable<ModelCollection<Banana>> userPlaylists(Urn user) {
        return profileApiPublic.get().userPlaylists(user);
    }

    @Override
    public Observable<ModelCollection<Banana>> userPlaylists(String nextPageLink) {
        return profileApiPublic.get().userPlaylists(nextPageLink);
    }

    @Override
    public Observable<ModelCollection<Banana>> userLikes(Urn user) {
        return profileApiMobile.get().userLikes(user);
    }

    @Override
    public Observable<ModelCollection<Banana>> userLikes(String nextPageLink) {
        return profileApiMobile.get().userLikes(nextPageLink);
    }

    @Override
    public Observable<ModelCollection<Banana>> userFollowings(Urn user) {
        return profileApiPublic.get().userFollowings(user);
    }

    @Override
    public Observable<ModelCollection<Banana>> userFollowings(String nextPageLink) {
        return profileApiPublic.get().userFollowings(nextPageLink);
    }

    @Override
    public Observable<ModelCollection<Banana>> userFollowers(Urn user) {
        return profileApiPublic.get().userFollowers(user);
    }

    @Override
    public Observable<ModelCollection<Banana>> userFollowers(String nextPageLink) {
        return profileApiPublic.get().userFollowers(nextPageLink);
    }

    @Override
    public Observable<ApiUserProfile> userProfile(Urn user) {
        return profileApiMobile.get().userProfile(user);
    }
}
