package com.soundcloud.android.profile;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Banana;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func1;

public class ProfileApiMobile implements ProfileApi {

    private final TypeToken<ModelCollection<BananaHolder>> holderToken =
            new TypeToken<ModelCollection<BananaHolder>>() {};

    private static final Func1<ModelCollection<BananaHolder>, ModelCollection<Banana>> HOLDER_TO_BANANA =
            new Func1<ModelCollection<BananaHolder>, ModelCollection<Banana>>() {
        @Override
        public ModelCollection<Banana> call(ModelCollection<BananaHolder> postItemHolderCollection) {
            final List<BananaHolder> collection = postItemHolderCollection.getCollection();
            List<Banana> posts = new ArrayList<>(collection.size());
            for (BananaHolder postHolder : collection) {
                final Optional<Banana> post = postHolder.getItem();
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
    public Observable<ModelCollection<Banana>> userPosts(Urn user) {
        return getPostsCollection(ApiEndpoints.USER_POSTS.path(user));
    }

    @Override
    public Observable<ModelCollection<Banana>> userPosts(String nextPageLink) {
        return getPostsCollection(nextPageLink);
    }

    @NotNull
    private Observable<ModelCollection<Banana>> getPostsCollection(String path) {
        final ApiRequest request = ApiRequest.get(path)
                .forPrivateApi(1)
                .addQueryParam(ApiRequest.Param.PAGE_SIZE, PAGE_SIZE)
                .build();

        return apiClientRx.mappedResponse(request, holderToken).map(HOLDER_TO_BANANA);
    }

    @Override
    public Observable<ModelCollection<Banana>> userPlaylists(Urn user) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Observable<ModelCollection<Banana>> userPlaylists(String nextPageLink) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Observable<ModelCollection<Banana>> userLikes(Urn user) {
        return getLikesCollection(ApiEndpoints.USER_LIKES.path(user));
    }

    @Override
    public Observable<ModelCollection<Banana>> userLikes(String nextPageLink) {
        return getLikesCollection(nextPageLink);
    }

    @NotNull
    private Observable<ModelCollection<Banana>> getLikesCollection(String path) {
        final ApiRequest request = ApiRequest.get(path)
                .forPrivateApi(1)
                .addQueryParam(ApiRequest.Param.PAGE_SIZE, PAGE_SIZE)
                .build();

        return apiClientRx.mappedResponse(request, holderToken).map(HOLDER_TO_BANANA);
    }

    @Override
    public Observable<ModelCollection<Banana>> userFollowings(Urn user) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Observable<ModelCollection<Banana>> userFollowings(String nextPageLink) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Observable<ModelCollection<Banana>> userFollowers(Urn user) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Observable<ModelCollection<Banana>> userFollowers(String nextPageLink) {
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
