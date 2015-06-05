package com.soundcloud.android.profile;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Consts;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.model.Urn;
import rx.Observable;


interface ProfileApi {

    @VisibleForTesting int PAGE_SIZE = Consts.LIST_PAGE_SIZE;

    Observable<PagedRemoteCollection> userPosts(Urn user);

    Observable<PagedRemoteCollection> userPosts(String nextPageLink);

    Observable<PagedRemoteCollection> userPlaylists(Urn user);

    Observable<PagedRemoteCollection> userPlaylists(String nextPageLink);

    Observable<PagedRemoteCollection> userLikes(Urn user);

    Observable<PagedRemoteCollection> userLikes(String nextPageLink);
}
