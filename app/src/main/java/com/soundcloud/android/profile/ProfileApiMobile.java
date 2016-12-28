package com.soundcloud.android.profile;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.reflect.TypeToken;
import org.jetbrains.annotations.NotNull;
import rx.Observable;

import javax.inject.Inject;

public class ProfileApiMobile implements ProfileApi {

    private final TypeToken<ModelCollection<ApiPostSource>> postSourceToken =
            new TypeToken<ModelCollection<ApiPostSource>>() {
            };

    private final TypeToken<ModelCollection<ApiPlayableSource>> playableSourceToken =
            new TypeToken<ModelCollection<ApiPlayableSource>>() {
            };

    private final TypeToken<ModelCollection<ApiPlaylistPost>> playlistPostToken =
            new TypeToken<ModelCollection<ApiPlaylistPost>>() {
            };

    private final ApiClientRx apiClientRx;

    @Inject
    public ProfileApiMobile(ApiClientRx apiClientRx) {
        this.apiClientRx = apiClientRx;
    }

    @Override
    public Observable<ModelCollection<ApiPostSource>> userPosts(Urn user) {
        return getPostsCollection(ApiEndpoints.USER_POSTS.path(user));
    }

    @Override
    public Observable<ModelCollection<ApiPostSource>> userPosts(String nextPageLink) {
        return getPostsCollection(nextPageLink);
    }

    @NotNull
    private Observable<ModelCollection<ApiPostSource>> getPostsCollection(String path) {
        final ApiRequest request = ApiRequest.get(path)
                                             .forPrivateApi()
                                             .addQueryParamIfAbsent(ApiRequest.Param.PAGE_SIZE, PAGE_SIZE)
                                             .build();

        return apiClientRx.mappedResponse(request, postSourceToken);
    }

    @Override
    public Observable<ModelCollection<ApiPlaylistPost>> userPlaylists(Urn user) {
        return getPlaylistsCollection(ApiEndpoints.USER_PLAYLISTS.path(user));
    }

    @Override
    public Observable<ModelCollection<ApiPlaylistPost>> userPlaylists(String nextPageLink) {
        return getPlaylistsCollection(nextPageLink);
    }

    @NotNull
    private Observable<ModelCollection<ApiPlaylistPost>> getPlaylistsCollection(String path) {
        final ApiRequest request = ApiRequest.get(path)
                                             .forPrivateApi()
                                             .addQueryParamIfAbsent(ApiRequest.Param.PAGE_SIZE, PAGE_SIZE)
                                             .build();

        return apiClientRx.mappedResponse(request, playlistPostToken);
    }

    @Override
    public Observable<ModelCollection<ApiUser>> userFollowings(Urn user) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Observable<ModelCollection<ApiUser>> userFollowings(String nextPageLink) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Observable<ModelCollection<ApiUser>> userFollowers(Urn user) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Observable<ModelCollection<ApiUser>> userFollowers(String nextPageLink) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Observable<ApiUserProfile> userProfile(Urn user) {
        final ApiRequest request = ApiRequest
                .get(ApiEndpoints.PROFILE.path(user))
                .forPrivateApi()
                .build();

        return apiClientRx.mappedResponse(request, ApiUserProfile.class);
    }

    @Override
    public Observable<ModelCollection<ApiPlayableSource>> userReposts(Urn user) {
        return getRepostsCollection(ApiEndpoints.USER_REPOSTS.path(user));
    }

    @Override
    public Observable<ModelCollection<ApiPlayableSource>> userReposts(String nextPageLink) {
        return getRepostsCollection(nextPageLink);
    }

    @NotNull
    private Observable<ModelCollection<ApiPlayableSource>> getRepostsCollection(String path) {
        final ApiRequest request = ApiRequest.get(path)
                                             .forPrivateApi()
                                             .addQueryParamIfAbsent(ApiRequest.Param.PAGE_SIZE, PAGE_SIZE)
                                             .build();

        return apiClientRx.mappedResponse(request, playableSourceToken);
    }

    @Override
    public Observable<ModelCollection<ApiPlayableSource>> userTracks(Urn user) {
        return getUserTracksCollection(ApiEndpoints.USER_TRACKS.path(user));
    }

    @Override
    public Observable<ModelCollection<ApiPlayableSource>> userTracks(String nextPageLink) {
        return getUserTracksCollection(nextPageLink);
    }

    @NotNull
    private Observable<ModelCollection<ApiPlayableSource>> getUserTracksCollection(String path) {
        final ApiRequest request = ApiRequest.get(path)
                                             .forPrivateApi()
                                             .addQueryParamIfAbsent(ApiRequest.Param.PAGE_SIZE, PAGE_SIZE)
                                             .build();

        return apiClientRx.mappedResponse(request, playableSourceToken);
    }

    @Override
    public Observable<ModelCollection<ApiPlaylistPost>> userAlbums(Urn user) {
        return getUserAlbumsCollection(ApiEndpoints.USER_ALBUMS.path(user));
    }

    @Override
    public Observable<ModelCollection<ApiPlaylistPost>> userAlbums(String nextPageLink) {
        return getUserAlbumsCollection(nextPageLink);
    }

    @NotNull
    private Observable<ModelCollection<ApiPlaylistPost>> getUserAlbumsCollection(String path) {
        final ApiRequest request = ApiRequest.get(path)
                                             .forPrivateApi()
                                             .addQueryParamIfAbsent(ApiRequest.Param.PAGE_SIZE, PAGE_SIZE)
                                             .build();

        return apiClientRx.mappedResponse(request, playlistPostToken);
    }

    @Override
    public Observable<ModelCollection<ApiPlayableSource>> userLikes(Urn user) {
        return getLikesCollection(ApiEndpoints.USER_LIKES.path(user));
    }

    @Override
    public Observable<ModelCollection<ApiPlayableSource>> userLikes(String nextPageLink) {
        return getLikesCollection(nextPageLink);
    }

    @NotNull
    private Observable<ModelCollection<ApiPlayableSource>> getLikesCollection(String path) {
        final ApiRequest request = ApiRequest.get(path)
                                             .forPrivateApi()
                                             .addQueryParamIfAbsent(ApiRequest.Param.PAGE_SIZE, PAGE_SIZE)
                                             .build();

        return apiClientRx.mappedResponse(request, playableSourceToken);
    }
}
