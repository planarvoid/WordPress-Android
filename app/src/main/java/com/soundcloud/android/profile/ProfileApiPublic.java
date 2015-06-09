package com.soundcloud.android.profile;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.legacy.PublicApiWrapper;
import com.soundcloud.android.api.legacy.model.CollectionHolder;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.SoundAssociation;
import com.soundcloud.android.api.legacy.model.SoundAssociationHolder;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiPlaylistLike;
import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.api.model.ApiPlaylistRepost;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiTrackLike;
import com.soundcloud.android.api.model.ApiTrackPost;
import com.soundcloud.android.api.model.ApiTrackRepost;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import org.jetbrains.annotations.NotNull;
import rx.Observable;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class ProfileApiPublic implements ProfileApi {

    private final ApiClientRx apiClientRx;

    private static final Func1<SoundAssociationHolder, ModelCollection<PropertySetSource>> SOUND_ASSOCIATIONS_TO_POSTS_COLLECTION = new Func1<SoundAssociationHolder, ModelCollection<PropertySetSource>>() {
        @Override
        public ModelCollection<PropertySetSource> call(SoundAssociationHolder postsHolder) {

            List<PropertySetSource> posts = new ArrayList<>(postsHolder.size());
            for (SoundAssociation soundAssociation : postsHolder){
                final Playable playable = soundAssociation.getPlayable();
                if (playable instanceof PublicApiTrack){
                    final ApiTrack apiTrack = ((PublicApiTrack) playable).toApiMobileTrack();
                    posts.add(soundAssociation.isRepost() ?
                            new ApiTrackRepost(apiTrack, soundAssociation.created_at) :
                            new ApiTrackPost(apiTrack));

                } else {
                    final ApiPlaylist apiPlaylist = ((PublicApiPlaylist) playable).toApiMobilePlaylist();
                    posts.add(soundAssociation.isRepost() ?
                            new ApiPlaylistRepost(apiPlaylist, soundAssociation.created_at) :
                            new ApiPlaylistPost(apiPlaylist));
                }
            }
            return new ModelCollection<>(posts, postsHolder.getNextHref());
        }
    };

    private static final Func1<SoundAssociationHolder, ModelCollection<PropertySetSource>> SOUND_ASSOCIATIONS_TO_LIKES_COLLECTION = new Func1<SoundAssociationHolder, ModelCollection<PropertySetSource>>() {
        @Override
        public ModelCollection<PropertySetSource> call(SoundAssociationHolder likesHolder) {

            List<PropertySetSource> likes = new ArrayList<>(likesHolder.size());
            for (SoundAssociation soundAssociation : likesHolder){
                final Playable playable = soundAssociation.getPlayable();
                if (playable instanceof PublicApiTrack){
                    likes.add(new ApiTrackLike(((PublicApiTrack) playable).toApiMobileTrack(), soundAssociation.created_at));

                } else {
                    likes.add(new ApiPlaylistLike(((PublicApiPlaylist) playable).toApiMobilePlaylist(), soundAssociation.created_at));
                }
            }
            return new ModelCollection<>(likes, likesHolder.getNextHref());
        }
    };

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
        return getPostsCollection(ApiEndpoints.USER_SOUNDS.path(user.getNumericId()));
    }

    @Override
    public Observable<ModelCollection<PropertySetSource>> userPosts(String pageLink) {
        return getPostsCollection(pageLink);
    }

    @Override
    public Observable<ModelCollection<PropertySetSource>> userLikes(Urn user) {
        return getLikesCollection(ApiEndpoints.USER_LIKES.path(user.getNumericId()));
    }

    @Override
    public Observable<ModelCollection<PropertySetSource>> userLikes(String pageLink) {
        return getLikesCollection(pageLink);
    }

    @Override
    public Observable<ModelCollection<ApiPlaylist>> userPlaylists(Urn user) {
        return getPlaylists(ApiEndpoints.USER_PLAYLISTS.path(user.getNumericId()));
    }

    @Override
    public Observable<ModelCollection<ApiPlaylist>> userPlaylists(String pageLink) {
        return getPlaylists(pageLink);
    }

    @Override
    public Observable<ModelCollection<ApiUser>> userFollowings(Urn user) {
        return getUsers(ApiEndpoints.USER_FOLLOWINGS.path(user.getNumericId()));
    }

    @Override
    public Observable<ModelCollection<ApiUser>> userFollowings(String pageLink) {
        return getUsers(pageLink);
    }

    @Override
    public Observable<ModelCollection<ApiUser>> userFollowers(Urn user) {
        return getUsers(ApiEndpoints.USER_FOLLOWERS.path(user.getNumericId()));
    }

    @Override
    public Observable<ModelCollection<ApiUser>> userFollowers(String pageLink) {
        return getUsers(pageLink);
    }

    @NotNull
    private Observable<ModelCollection<PropertySetSource>> getPostsCollection(String path) {
        final ApiRequest request = ApiRequest.get(path)
                .forPublicApi()
                .addQueryParam(PublicApiWrapper.LINKED_PARTITIONING, "1")
                .addQueryParam(ApiRequest.Param.PAGE_SIZE, PAGE_SIZE)
                .build();

        return apiClientRx.mappedResponse(request, SoundAssociationHolder.class)
                .map(SOUND_ASSOCIATIONS_TO_POSTS_COLLECTION);
    }

    @NotNull
    private Observable<ModelCollection<PropertySetSource>> getLikesCollection(String path) {
        final ApiRequest request = ApiRequest.get(path)
                .forPublicApi()
                .addQueryParam(PublicApiWrapper.LINKED_PARTITIONING, "1")
                .addQueryParam(ApiRequest.Param.PAGE_SIZE, PAGE_SIZE)
                .build();

        return apiClientRx.mappedResponse(request, SoundAssociationHolder.class)
                .map(SOUND_ASSOCIATIONS_TO_LIKES_COLLECTION);
    }

    @NotNull
    private Observable<ModelCollection<ApiPlaylist>> getPlaylists(String path) {
        final ApiRequest request = ApiRequest.get(path)
                .forPublicApi()
                .addQueryParam(PublicApiWrapper.LINKED_PARTITIONING, "1")
                .addQueryParam(ApiRequest.Param.PAGE_SIZE, PAGE_SIZE)
                .build();

        return apiClientRx.mappedResponse(request, playlistHolderToken)
                .map(PLAYLISTS_TO_COLLECTION);
    }

    @NotNull
    private Observable<ModelCollection<ApiUser>> getUsers(String path) {
        final ApiRequest request = ApiRequest.get(path)
                .forPublicApi()
                .addQueryParam(PublicApiWrapper.LINKED_PARTITIONING, "1")
                .addQueryParam(ApiRequest.Param.PAGE_SIZE, PAGE_SIZE)
                .build();

        return apiClientRx.mappedResponse(request, userHolderToken)
                .map(USERS_TO_COLLECTION);
    }

    private final TypeToken<CollectionHolder<PublicApiPlaylist>> playlistHolderToken = new TypeToken<CollectionHolder<PublicApiPlaylist>>() {};

    private final TypeToken<CollectionHolder<PublicApiUser>> userHolderToken = new TypeToken<CollectionHolder<PublicApiUser>>() {};

}
