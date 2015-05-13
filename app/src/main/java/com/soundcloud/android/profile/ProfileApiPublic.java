package com.soundcloud.android.profile;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.legacy.PublicApiWrapper;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.SoundAssociation;
import com.soundcloud.android.api.legacy.model.SoundAssociationHolder;
import com.soundcloud.android.api.model.PagedRemoteCollection;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
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
    private final Func1<SoundAssociationHolder, PagedRemoteCollection> soundAssciationsToCollection = new Func1<SoundAssociationHolder, PagedRemoteCollection>() {
        @Override
        public PagedRemoteCollection call(SoundAssociationHolder soundAssociations) {
            return new PagedRemoteCollection(soundAssociations.collection, soundAssociations.next_href);
        }
    };

    private final StoreTracksCommand storeTracksCommand;
    private final StorePlaylistsCommand storePlaylistsCommand;

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

    @Inject
    public ProfileApiPublic(ApiClientRx apiClientRx, StoreTracksCommand storeTracksCommand, StorePlaylistsCommand storePlaylistsCommand) {
        this.apiClientRx = apiClientRx;
        this.storeTracksCommand = storeTracksCommand;
        this.storePlaylistsCommand = storePlaylistsCommand;
    }

    @Override
    public Observable<PagedRemoteCollection> userPosts(Urn user) {
        return getSoundAssociations(ApiEndpoints.USER_SOUNDS.path(user.getNumericId()));
    }

    @Override
    public Observable<PagedRemoteCollection> userPosts(String pageLink) {
        return getSoundAssociations(pageLink);
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
                .map(soundAssciationsToCollection);
    }

}
