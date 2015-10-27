package com.soundcloud.android.profile;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.PropertySetSource;
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

    private final TypeToken<ModelCollection<ApiPostHolder>> apiPostHolderToken =
            new TypeToken<ModelCollection<ApiPostHolder>>() {};

    private final TypeToken<ModelCollection<ApiLikeHolder>> apiLikeHolderToken =
            new TypeToken<ModelCollection<ApiLikeHolder>>() {};

    private static final Func1<ModelCollection<ApiPostHolder>, ModelCollection<PropertySetSource>> POST_HOLDER_TO_POST_COLLECTION =
            new Func1<ModelCollection<ApiPostHolder>, ModelCollection<PropertySetSource>>() {
        @Override
        public ModelCollection<PropertySetSource> call(ModelCollection<ApiPostHolder> postItemHolderCollection) {
            final List<ApiPostHolder> collection = postItemHolderCollection.getCollection();
            List<PropertySetSource> posts = new ArrayList<>(collection.size());
            for (ApiPostHolder postHolder : collection) {
                final Optional<PropertySetSource> post = postHolder.getPost();
                if (post.isPresent()) {
                    posts.add(post.get());
                }
            }
            return new ModelCollection(posts, postItemHolderCollection.getLinks());
        }
    };

    private static final Func1<ModelCollection<ApiLikeHolder>, ModelCollection<PropertySetSource>> LIKE_HOLDER_TO_LIKES_COLLECTION =
            new Func1<ModelCollection<ApiLikeHolder>, ModelCollection<PropertySetSource>>() {
                @Override
                public ModelCollection<PropertySetSource> call(ModelCollection<ApiLikeHolder> postItemHolderCollection) {
                    final List<ApiLikeHolder> collection = postItemHolderCollection.getCollection();
                    List<PropertySetSource> likes = new ArrayList<>(collection.size());
                    for (ApiLikeHolder postHolder : collection) {
                        final Optional<PropertySetSource> like = postHolder.getLike();
                        if (like.isPresent()) {
                            likes.add(like.get());
                        }
                    }
                    return new ModelCollection(likes, postItemHolderCollection.getLinks());
                }
            };


    private final ApiClientRx apiClientRx;

    @Inject
    public ProfileApiMobile(ApiClientRx apiClientRx) {
        this.apiClientRx = apiClientRx;
    }

    @Override
    public Observable<ModelCollection<PropertySetSource>> userPosts(Urn user) {
        return getPostsCollection(ApiEndpoints.USER_POSTS.path(user));
    }

    @Override
    public Observable<ModelCollection<PropertySetSource>> userPosts(String nextPageLink) {
        return getPostsCollection(nextPageLink);
    }

    @NotNull
    private Observable<ModelCollection<PropertySetSource>> getPostsCollection(String path) {
        final ApiRequest request = ApiRequest.get(path)
                .forPrivateApi(1)
                .addQueryParam(ApiRequest.Param.PAGE_SIZE, PAGE_SIZE)
                .build();

        return apiClientRx.mappedResponse(request, apiPostHolderToken).map(POST_HOLDER_TO_POST_COLLECTION);
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
    public Observable<ModelCollection<PropertySetSource>> userLikes(Urn user) {
        return getLikesCollection(ApiEndpoints.USER_LIKES.path(user));
    }

    @Override
    public Observable<ModelCollection<PropertySetSource>> userLikes(String nextPageLink) {
        return getLikesCollection(nextPageLink);
    }

    @NotNull
    private Observable<ModelCollection<PropertySetSource>> getLikesCollection(String path) {
        final ApiRequest request = ApiRequest.get(path)
                .forPrivateApi(1)
                .addQueryParam(ApiRequest.Param.PAGE_SIZE, PAGE_SIZE)
                .build();

        return apiClientRx.mappedResponse(request, apiLikeHolderToken).map(LIKE_HOLDER_TO_LIKES_COLLECTION);
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
}
