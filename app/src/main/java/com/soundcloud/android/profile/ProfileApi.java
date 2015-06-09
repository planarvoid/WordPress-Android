package com.soundcloud.android.profile;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Consts;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import rx.Observable;


interface ProfileApi {

    @VisibleForTesting int PAGE_SIZE = Consts.LIST_PAGE_SIZE;

    Observable<ModelCollection<PropertySetSource>> userPosts(Urn user);

    Observable<ModelCollection<PropertySetSource>> userPosts(String nextPageLink);

    Observable<ModelCollection<ApiPlaylist>> userPlaylists(Urn user);

    Observable<ModelCollection<ApiPlaylist>> userPlaylists(String nextPageLink);

    Observable<ModelCollection<PropertySetSource>> userLikes(Urn user);

    Observable<ModelCollection<PropertySetSource>> userLikes(String nextPageLink);

    Observable<ModelCollection<ApiUser>> userFollowings(Urn user);

    Observable<ModelCollection<ApiUser>> userFollowings(String nextPageLink);

    Observable<ModelCollection<ApiUser>> userFollowers(Urn user);

    Observable<ModelCollection<ApiUser>> userFollowers(String nextPageLink);
}
