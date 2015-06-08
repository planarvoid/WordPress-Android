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
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistRecord;
import com.soundcloud.android.tracks.TrackRecord;
import org.jetbrains.annotations.NotNull;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class ProfileApiPublic implements ProfileApi {

    private final ApiClientRx apiClientRx;

    private static final Func1<CollectionHolder<? extends PropertySetSource>, PagedRemoteCollection> HOLDER_TO_COLLECTION = new Func1<CollectionHolder<? extends PropertySetSource>, PagedRemoteCollection>() {
        @Override
        public PagedRemoteCollection call(CollectionHolder<? extends PropertySetSource> playlistsHolder) {
            return new PagedRemoteCollection(playlistsHolder.collection, playlistsHolder.next_href);
        }
    };

    private final StoreTracksCommand storeTracksCommand;
    private final StorePlaylistsCommand storePlaylistsCommand;
    private final StoreUsersCommand storeUsersCommand;

    private final Action1<? super SoundAssociationHolder> writeSoundAssociationsToStorage = new Action1<SoundAssociationHolder>() {
        @Override
        public void call(SoundAssociationHolder collection) {
            List<TrackRecord> tracks = new ArrayList<>();
            List<PlaylistRecord> playlists = new ArrayList<>();
            for (SoundAssociation entity : collection){
                final Playable playable = entity.getPlayable();
                if (playable instanceof PublicApiTrack){
                    tracks.add((TrackRecord) playable);
                } else if (playable instanceof PublicApiPlaylist){
                    playlists.add((PlaylistRecord) playable);
                }
            }

            if (!tracks.isEmpty()){
                storeTracksCommand.call(tracks);
            }
            if (!playlists.isEmpty()){
                storePlaylistsCommand.call(playlists);
            }
        }
    };

    private final Action1<? super CollectionHolder<PublicApiPlaylist>> writePlaylistsToStorage = new Action1<CollectionHolder<PublicApiPlaylist>>() {
        @Override
        public void call(CollectionHolder<PublicApiPlaylist> publicApiPlaylists) {
            if (!publicApiPlaylists.isEmpty()){
                storePlaylistsCommand.call(publicApiPlaylists);
            }
        }
    };

    private final Action1<? super CollectionHolder<PublicApiUser>> writeUsersToStorage = new Action1<CollectionHolder<PublicApiUser>>() {
        @Override
        public void call(CollectionHolder<PublicApiUser> publicApiUsers) {
            if (!publicApiUsers.isEmpty()){
                storeUsersCommand.call(publicApiUsers);
            }
        }
    };

    @Inject
    public ProfileApiPublic(ApiClientRx apiClientRx, StoreTracksCommand storeTracksCommand, StorePlaylistsCommand storePlaylistsCommand, StoreUsersCommand storeUsersCommand) {
        this.apiClientRx = apiClientRx;
        this.storeTracksCommand = storeTracksCommand;
        this.storePlaylistsCommand = storePlaylistsCommand;
        this.storeUsersCommand = storeUsersCommand;
    }

    @Override
    public Observable<PagedRemoteCollection> userPosts(Urn user) {
        return getSoundAssociations(ApiEndpoints.USER_SOUNDS.path(user.getNumericId()));
    }

    @Override
    public Observable<PagedRemoteCollection> userPosts(String pageLink) {
        return getSoundAssociations(pageLink);
    }

    @Override
    public Observable<PagedRemoteCollection> userLikes(Urn user) {
        return getSoundAssociations(ApiEndpoints.USER_LIKES.path(user.getNumericId()));
    }

    @Override
    public Observable<PagedRemoteCollection> userLikes(String pageLink) {
        return getSoundAssociations(pageLink);
    }

    @Override
    public Observable<PagedRemoteCollection> userPlaylists(Urn user) {
        return getPlaylists(ApiEndpoints.USER_PLAYLISTS.path(user.getNumericId()));
    }

    @Override
    public Observable<PagedRemoteCollection> userPlaylists(String pageLink) {
        return getPlaylists(pageLink);
    }

    @Override
    public Observable<PagedRemoteCollection> userFollowings(Urn user) {
        return getUsers(ApiEndpoints.USER_FOLLOWINGS.path(user.getNumericId()));
    }

    @Override
    public Observable<PagedRemoteCollection> userFollowings(String pageLink) {
        return getUsers(pageLink);
    }

    @Override
    public Observable<PagedRemoteCollection> userFollowers(Urn user) {
        return getUsers(ApiEndpoints.USER_FOLLOWERS.path(user.getNumericId()));
    }

    @Override
    public Observable<PagedRemoteCollection> userFollowers(String pageLink) {
        return getUsers(pageLink);
    }

    @NotNull
    private Observable<PagedRemoteCollection> getSoundAssociations(String path) {
        final ApiRequest request = ApiRequest.get(path)
                .forPublicApi()
                .addQueryParam(PublicApiWrapper.LINKED_PARTITIONING, "1")
                .addQueryParam(ApiRequest.Param.PAGE_SIZE, PAGE_SIZE)
                .build();

        return apiClientRx.mappedResponse(request, SoundAssociationHolder.class)
                .doOnNext(writeSoundAssociationsToStorage)
                .map(HOLDER_TO_COLLECTION);
    }

    @NotNull
    private Observable<PagedRemoteCollection> getPlaylists(String path) {
        final ApiRequest request = ApiRequest.get(path)
                .forPublicApi()
                .addQueryParam(PublicApiWrapper.LINKED_PARTITIONING, "1")
                .addQueryParam(ApiRequest.Param.PAGE_SIZE, PAGE_SIZE)
                .build();

        return apiClientRx.mappedResponse(request, playlistHolderToken)
                .doOnNext(writePlaylistsToStorage)
                .map(HOLDER_TO_COLLECTION);
    }

    @NotNull
    private Observable<PagedRemoteCollection> getUsers(String path) {
        final ApiRequest request = ApiRequest.get(path)
                .forPublicApi()
                .addQueryParam(PublicApiWrapper.LINKED_PARTITIONING, "1")
                .addQueryParam(ApiRequest.Param.PAGE_SIZE, PAGE_SIZE)
                .build();

        return apiClientRx.mappedResponse(request, userHolderToken)
                .doOnNext(writeUsersToStorage)
                .map(HOLDER_TO_COLLECTION);
    }

    private final TypeToken<CollectionHolder<PublicApiPlaylist>> playlistHolderToken = new TypeToken<CollectionHolder<PublicApiPlaylist>>() {};

    private final TypeToken<CollectionHolder<PublicApiUser>> userHolderToken = new TypeToken<CollectionHolder<PublicApiUser>>() {};

}
