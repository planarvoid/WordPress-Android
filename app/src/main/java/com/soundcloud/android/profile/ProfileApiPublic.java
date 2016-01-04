package com.soundcloud.android.profile;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.model.CollectionHolder;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.reflect.TypeToken;
import org.jetbrains.annotations.NotNull;
import rx.Observable;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class ProfileApiPublic implements ProfileApi {

    private final ApiClientRx apiClientRx;

    private static final Func1<CollectionHolder<PublicApiPlaylist>, ModelCollection<ApiPlaylist>> PLAYLISTS_TO_COLLECTION = new Func1<CollectionHolder<PublicApiPlaylist>, ModelCollection<ApiPlaylist>>() {
        @Override
        public ModelCollection<ApiPlaylist> call(CollectionHolder<PublicApiPlaylist> playlistsHolder) {
            List<ApiPlaylist> playlists = new ArrayList<>(playlistsHolder.size());
            for (PublicApiPlaylist publicApiPlaylist : playlistsHolder){
                playlists.add(publicApiPlaylist.toApiMobilePlaylist());
            }
            return new ModelCollection<>(playlists, playlistsHolder.getNextHref());
        }
    };

    private static final Func1<CollectionHolder<PublicApiUser>, ModelCollection<ApiUser>> USERS_TO_COLLECTION = new Func1<CollectionHolder<PublicApiUser>, ModelCollection<ApiUser>>() {
        @Override
        public ModelCollection<ApiUser> call(CollectionHolder<PublicApiUser> usersHolder) {
            List<ApiUser> users = new ArrayList<>(usersHolder.size());
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
    public Observable<ModelCollection<PropertySetSource>> userPosts(Urn user) {
        throw new UnsupportedOperationException("User posts are no longer supported via Public API");
    }

    @Override
    public Observable<ModelCollection<PropertySetSource>> userPosts(String pageLink) {
        throw new UnsupportedOperationException("User posts are no longer supported via Public API");
    }

    @Override
    public Observable<ModelCollection<PropertySetSource>> userLikes(Urn user) {
        throw new UnsupportedOperationException("User likes are no longer supported via Public API");
    }

    @Override
    public Observable<ModelCollection<PropertySetSource>> userLikes(String pageLink) {
        throw new UnsupportedOperationException("User likes are no longer supported via Public API");
    }

    @Override
    public Observable<ModelCollection<ApiPlaylist>> userPlaylists(Urn user) {
        return getPlaylists(ApiEndpoints.LEGACY_USER_PLAYLISTS.path(user.getNumericId()));
    }

    @Override
    public Observable<ModelCollection<ApiPlaylist>> userPlaylists(String pageLink) {
        return getPlaylists(pageLink);
    }

    @Override
    public Observable<ModelCollection<ApiUser>> userFollowings(Urn user) {
        return getUsers(ApiEndpoints.LEGACY_USER_FOLLOWINGS.path(user.getNumericId()));
    }

    @Override
    public Observable<ModelCollection<ApiUser>> userFollowings(String pageLink) {
        return getUsers(pageLink);
    }

    @Override
    public Observable<ModelCollection<ApiUser>> userFollowers(Urn user) {
        return getUsers(ApiEndpoints.LEGACY_USER_FOLLOWERS.path(user.getNumericId()));
    }

    @Override
    public Observable<ModelCollection<ApiUser>> userFollowers(String pageLink) {
        return getUsers(pageLink);
    }

    @Override
    public Observable<ApiUserProfile> userProfile(Urn user) {
        throw new UnsupportedOperationException("User Profile will not be supported by Public API");
    }

    @NotNull
    private Observable<ModelCollection<ApiPlaylist>> getPlaylists(String path) {
        final ApiRequest request = ApiRequest.get(path)
                .forPublicApi()
                .addQueryParam(PublicApi.LINKED_PARTITIONING, "1")
                .addQueryParam(ApiRequest.Param.PAGE_SIZE, PAGE_SIZE)
                .build();

        return apiClientRx.mappedResponse(request, playlistHolderToken)
                .map(PLAYLISTS_TO_COLLECTION);
    }

    @NotNull
    private Observable<ModelCollection<ApiUser>> getUsers(String path) {
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
