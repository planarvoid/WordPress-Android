package com.soundcloud.android.profile;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.model.CollectionHolder;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Banana;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func1;

public class ProfileApiPublic implements ProfileApi {

    private final ApiClientRx apiClientRx;

    private static final Func1<CollectionHolder<PublicApiPlaylist>, ModelCollection<Banana>> PLAYLISTS_TO_COLLECTION = new Func1<CollectionHolder<PublicApiPlaylist>, ModelCollection<Banana>>() {
        @Override
        public ModelCollection<Banana> call(CollectionHolder<PublicApiPlaylist> playlistsHolder) {
            List<Banana> playlists = new ArrayList<>(playlistsHolder.size());
            for (PublicApiPlaylist publicApiPlaylist : playlistsHolder){
                playlists.add(publicApiPlaylist.toApiMobilePlaylist());
            }
            return new ModelCollection<>(playlists, playlistsHolder.getNextHref());
        }
    };

    private static final Func1<CollectionHolder<PublicApiUser>, ModelCollection<Banana>> USERS_TO_COLLECTION = new Func1<CollectionHolder<PublicApiUser>, ModelCollection<Banana>>() {
        @Override
        public ModelCollection<Banana> call(CollectionHolder<PublicApiUser> usersHolder) {
            List<Banana> users = new ArrayList<>(usersHolder.size());
            for (PublicApiUser publicApiUser : usersHolder){
                users.add(publicApiUser.toApiMobileUser());
            }
            return new ModelCollection<>(users, usersHolder.getNextHref());
        }
    };

    @Inject
    public ProfileApiPublic(ApiClientRx apiClientRx) {
        this.apiClientRx = apiClientRx;
    }

    @Override
    public Observable<ModelCollection<Banana>> userPosts(Urn user) {
        throw new UnsupportedOperationException("User posts are no longer supported via Public API");
    }

    @Override
    public Observable<ModelCollection<Banana>> userPosts(String pageLink) {
        throw new UnsupportedOperationException("User posts are no longer supported via Public API");
    }

    @Override
    public Observable<ModelCollection<Banana>> userLikes(Urn user) {
        throw new UnsupportedOperationException("User likes are no longer supported via Public API");
    }

    @Override
    public Observable<ModelCollection<Banana>> userLikes(String pageLink) {
        throw new UnsupportedOperationException("User likes are no longer supported via Public API");
    }

    @Override
    public Observable<ModelCollection<Banana>> userPlaylists(Urn user) {
        return getPlaylists(ApiEndpoints.LEGACY_USER_PLAYLISTS.path(user.getNumericId()));
    }

    @Override
    public Observable<ModelCollection<Banana>> userPlaylists(String pageLink) {
        return getPlaylists(pageLink);
    }

    @Override
    public Observable<ModelCollection<Banana>> userFollowings(Urn user) {
        return getUsers(ApiEndpoints.LEGACY_USER_FOLLOWINGS.path(user.getNumericId()));
    }

    @Override
    public Observable<ModelCollection<Banana>> userFollowings(String pageLink) {
        return getUsers(pageLink);
    }

    @Override
    public Observable<ModelCollection<Banana>> userFollowers(Urn user) {
        return getUsers(ApiEndpoints.LEGACY_USER_FOLLOWERS.path(user.getNumericId()));
    }

    @Override
    public Observable<ModelCollection<Banana>> userFollowers(String pageLink) {
        return getUsers(pageLink);
    }

    @Override
    public Observable<ApiUserProfile> userProfile(Urn user) {
        throw new UnsupportedOperationException("User Profile will not be supported by Public API");
    }

    @NotNull
    private Observable<ModelCollection<Banana>> getPlaylists(String path) {
        final ApiRequest request = ApiRequest.get(path)
                .forPublicApi()
                .addQueryParam(PublicApi.LINKED_PARTITIONING, "1")
                .addQueryParam(ApiRequest.Param.PAGE_SIZE, PAGE_SIZE)
                .build();

        return apiClientRx.mappedResponse(request, playlistHolderToken)
                .map(PLAYLISTS_TO_COLLECTION);
    }

    @NotNull
    private Observable<ModelCollection<Banana>> getUsers(String path) {
        final ApiRequest request = ApiRequest.get(path)
                .forPublicApi()
                .addQueryParam(PublicApi.LINKED_PARTITIONING, "1")
                .addQueryParam(ApiRequest.Param.PAGE_SIZE, PAGE_SIZE)
                .build();

        return apiClientRx.mappedResponse(request, userHolderToken)
                .map(USERS_TO_COLLECTION);
    }

    private final TypeToken<CollectionHolder<PublicApiPlaylist>> playlistHolderToken = new TypeToken<CollectionHolder<PublicApiPlaylist>>() {};

    private final TypeToken<CollectionHolder<PublicApiUser>> userHolderToken = new TypeToken<CollectionHolder<PublicApiUser>>() {};

}
