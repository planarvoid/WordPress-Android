package com.soundcloud.android.profile;

import android.support.annotation.VisibleForTesting;

import com.soundcloud.android.Consts;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Banana;
import com.soundcloud.android.model.Urn;

import rx.Observable;


interface ProfileApi {

    @VisibleForTesting int PAGE_SIZE = Consts.LIST_PAGE_SIZE;

    Observable<ModelCollection<Banana>> userPosts(Urn user);

    Observable<ModelCollection<Banana>> userPosts(String nextPageLink);

    Observable<ModelCollection<Banana>> userPlaylists(Urn user);

    Observable<ModelCollection<Banana>> userPlaylists(String nextPageLink);

    Observable<ModelCollection<Banana>> userLikes(Urn user);

    Observable<ModelCollection<Banana>> userLikes(String nextPageLink);

    Observable<ModelCollection<Banana>> userFollowings(Urn user);

    Observable<ModelCollection<Banana>> userFollowings(String nextPageLink);

    Observable<ModelCollection<Banana>> userFollowers(Urn user);

    Observable<ModelCollection<Banana>> userFollowers(String nextPageLink);

    Observable<ApiUserProfile> userProfile(Urn user);
}
