package com.soundcloud.android.discovery.systemplaylist;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.ApiClientRxV2;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.discovery.DiscoveryReadableStorage;
import com.soundcloud.android.discovery.DiscoveryWritableStorage;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackRepository;
import io.reactivex.Maybe;
import io.reactivex.Scheduler;

import javax.inject.Inject;
import javax.inject.Named;

class SystemPlaylistOperations {

    private final ApiClientRxV2 apiClientRx;
    private final StoreTracksCommand storeTracksCommand;
    private final Scheduler scheduler;
    private final DiscoveryWritableStorage discoveryWritableStorage;
    private final DiscoveryReadableStorage discoveryReadableStorage;
    private final TrackRepository trackRepository;

    @Inject
    SystemPlaylistOperations(ApiClientRxV2 apiClientRx,
                             StoreTracksCommand storeTracksCommand,
                             @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler,
                             DiscoveryWritableStorage discoveryWritableStorage,
                             DiscoveryReadableStorage discoveryReadableStorage,
                             TrackRepository trackRepository) {
        this.apiClientRx = apiClientRx;
        this.storeTracksCommand = storeTracksCommand;
        this.scheduler = scheduler;
        this.discoveryWritableStorage = discoveryWritableStorage;
        this.discoveryReadableStorage = discoveryReadableStorage;
        this.trackRepository = trackRepository;
    }

    Maybe<SystemPlaylist> fetchSystemPlaylist(Urn urn) {
        return discoveryReadableStorage.systemPlaylistEntity(urn)
                                       .flatMap(systemPlaylistEntity -> trackRepository.trackListFromUrns(systemPlaylistEntity.trackUrns())
                                                                                       .toMaybe()
                                                                                       .map(tracks -> SystemPlaylistMapper.map(systemPlaylistEntity, tracks)))
                                       .switchIfEmpty(fetchSystemPlaylistFromApi(urn));
    }

    private Maybe<SystemPlaylist> fetchSystemPlaylistFromApi(Urn urn) {
        ApiRequest request = ApiRequest.get(ApiEndpoints.SYSTEM_PLAYLISTS.path(urn))
                                       .forPrivateApi()
                                       .build();

        return apiClientRx.mappedResponse(request, ApiSystemPlaylist.class)
                          .subscribeOn(scheduler)
                          .doOnSuccess(this::storeSystemPlaylist)
                          .map(SystemPlaylistMapper::map)
                          .toMaybe();
    }

    private void storeSystemPlaylist(ApiSystemPlaylist apiSystemPlaylist) {
        storeTracksCommand.call(apiSystemPlaylist.tracks());
        discoveryWritableStorage.storeSystemPlaylist(apiSystemPlaylist);
    }
}
