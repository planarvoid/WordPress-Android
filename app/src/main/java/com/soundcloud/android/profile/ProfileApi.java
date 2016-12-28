package com.soundcloud.android.profile;

import com.soundcloud.android.Consts;
import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import rx.Observable;

import android.support.annotation.VisibleForTesting;


interface ProfileApi {

    @VisibleForTesting int PAGE_SIZE = Consts.LIST_PAGE_SIZE;

    Observable<ModelCollection<ApiPostSource>> userPosts(Urn user);

    Observable<ModelCollection<ApiPostSource>> userPosts(String nextPageLink);

    Observable<ModelCollection<ApiPlaylistPost>> userPlaylists(Urn user);

    Observable<ModelCollection<ApiPlaylistPost>> userPlaylists(String nextPageLink);

    Observable<ModelCollection<ApiPlayableSource>> userLikes(Urn user);

    Observable<ModelCollection<ApiPlayableSource>> userLikes(String nextPageLink);

    Observable<ModelCollection<ApiUser>> userFollowings(Urn user);

    Observable<ModelCollection<ApiUser>> userFollowings(String nextPageLink);

    Observable<ModelCollection<ApiUser>> userFollowers(Urn user);

    Observable<ModelCollection<ApiUser>> userFollowers(String nextPageLink);

    Observable<ApiUserProfile> userProfile(Urn user);

    Observable<ModelCollection<ApiPlayableSource>> userReposts(Urn user);

    Observable<ModelCollection<ApiPlayableSource>> userReposts(String nextPageLink);

    Observable<ModelCollection<ApiPlayableSource>> userTracks(Urn user);

    Observable<ModelCollection<ApiPlayableSource>> userTracks(String nextPageLink);

    Observable<ModelCollection<ApiPlaylistPost>> userAlbums(Urn user);

    Observable<ModelCollection<ApiPlaylistPost>> userAlbums(String nextPageLink);

}
