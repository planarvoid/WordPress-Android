package com.soundcloud.android.profile;

import com.soundcloud.android.Consts;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.ApiEntityHolder;
import com.soundcloud.android.model.Urn;
import rx.Observable;

import android.support.annotation.VisibleForTesting;


interface ProfileApi {

    @VisibleForTesting int PAGE_SIZE = Consts.LIST_PAGE_SIZE;

    Observable<ModelCollection<ApiEntityHolder>> userPosts(Urn user);

    Observable<ModelCollection<ApiEntityHolder>> userPosts(String nextPageLink);

    Observable<ModelCollection<ApiPlaylist>> userPlaylists(Urn user);

    Observable<ModelCollection<ApiPlaylist>> userPlaylists(String nextPageLink);

    Observable<ModelCollection<ApiEntityHolder>> userLikes(Urn user);

    Observable<ModelCollection<ApiEntityHolder>> userLikes(String nextPageLink);

    Observable<ModelCollection<ApiUser>> userFollowings(Urn user);

    Observable<ModelCollection<ApiUser>> userFollowings(String nextPageLink);

    Observable<ModelCollection<ApiUser>> userFollowers(Urn user);

    Observable<ModelCollection<ApiUser>> userFollowers(String nextPageLink);

    Observable<ApiUserProfile> userProfile(Urn user);
}
