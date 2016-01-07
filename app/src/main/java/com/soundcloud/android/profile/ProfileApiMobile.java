package com.soundcloud.android.profile;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.ApiEntityHolder;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.reflect.TypeToken;
import org.jetbrains.annotations.NotNull;
import rx.Observable;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class ProfileApiMobile implements ProfileApi {

    private final TypeToken<ModelCollection<ApiEntityHolderSource>> holderToken =
            new TypeToken<ModelCollection<ApiEntityHolderSource>>() {};

    private static final Func1<ModelCollection<ApiEntityHolderSource>, ModelCollection<ApiEntityHolder>> SOURCE_TO_HOLDER =
            new Func1<ModelCollection<ApiEntityHolderSource>, ModelCollection<ApiEntityHolder>>() {
        @Override
        public ModelCollection<ApiEntityHolder> call(ModelCollection<ApiEntityHolderSource> postItemHolderCollection) {
            final List<ApiEntityHolderSource> collection = postItemHolderCollection.getCollection();
            List<ApiEntityHolder> posts = new ArrayList<>(collection.size());
            for (ApiEntityHolderSource postHolder : collection) {
                final Optional<ApiEntityHolder> post = postHolder.getEntityHolder();
                if (post.isPresent()) {
                    posts.add(post.get());
                }
            }
            return new ModelCollection(posts, postItemHolderCollection.getLinks());
        }
    };

    private final ApiClientRx apiClientRx;

    @Inject
    public ProfileApiMobile(ApiClientRx apiClientRx) {
        this.apiClientRx = apiClientRx;
    }

    @Override
    public Observable<ModelCollection<ApiEntityHolder>> userPosts(Urn user) {
        return getPostsCollection(ApiEndpoints.USER_POSTS.path(user));
    }

    @Override
    public Observable<ModelCollection<ApiEntityHolder>> userPosts(String nextPageLink) {
        return getPostsCollection(nextPageLink);
    }

    @NotNull
    private Observable<ModelCollection<ApiEntityHolder>> getPostsCollection(String path) {
        final ApiRequest request = ApiRequest.get(path)
                .forPrivateApi(1)
                .addQueryParam(ApiRequest.Param.PAGE_SIZE, PAGE_SIZE)
                .build();

        return apiClientRx.mappedResponse(request, holderToken).map(SOURCE_TO_HOLDER);
    }

    @Override
    public Observable<ModelCollection<ApiPlaylist>> userPlaylists(Urn user) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Observable<ModelCollection<ApiPlaylist>> userPlaylists(String nextPageLink) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Observable<ModelCollection<ApiEntityHolder>> userLikes(Urn user) {
        return getLikesCollection(ApiEndpoints.USER_LIKES.path(user));
    }

    @Override
    public Observable<ModelCollection<ApiEntityHolder>> userLikes(String nextPageLink) {
        return getLikesCollection(nextPageLink);
    }

    @NotNull
    private Observable<ModelCollection<ApiEntityHolder>> getLikesCollection(String path) {
        final ApiRequest request = ApiRequest.get(path)
                .forPrivateApi(1)
                .addQueryParam(ApiRequest.Param.PAGE_SIZE, PAGE_SIZE)
                .build();

        return apiClientRx.mappedResponse(request, holderToken).map(SOURCE_TO_HOLDER);
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
                .forPrivateApi(1)
                .build();

        return apiClientRx.mappedResponse(request, ApiUserProfile.class);
    }
}
