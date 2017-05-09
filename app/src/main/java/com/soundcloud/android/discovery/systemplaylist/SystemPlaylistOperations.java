package com.soundcloud.android.discovery.systemplaylist;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.ApiClientRxV2;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import io.reactivex.Scheduler;
import io.reactivex.Single;

import javax.inject.Inject;
import javax.inject.Named;

class SystemPlaylistOperations {

    private final ApiClientRxV2 apiClientRx;
    private final StoreTracksCommand storeTracksCommand;
    private final Scheduler scheduler;

    @Inject
    SystemPlaylistOperations(ApiClientRxV2 apiClientRx,
                             StoreTracksCommand storeTracksCommand,
                             @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler) {
        this.apiClientRx = apiClientRx;
        this.storeTracksCommand = storeTracksCommand;
        this.scheduler = scheduler;
    }

    Single<SystemPlaylist> fetchSystemPlaylist(Urn urn) {
        ApiRequest request = ApiRequest.get(ApiEndpoints.SYSTEM_PLAYLISTS.path(urn))
                                       .forPrivateApi()
                                       .build();

        return apiClientRx.mappedResponse(request, ApiSystemPlaylist.class)
                          .subscribeOn(scheduler)
                          .doOnSuccess(this::storeTracks)
                          .map(SystemPlaylistMapper::map);
    }

    private void storeTracks(ApiSystemPlaylist apiSystemPlaylist) {
        storeTracksCommand.call(apiSystemPlaylist.tracks());
    }
}
