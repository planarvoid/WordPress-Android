package com.soundcloud.android.sync;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.propeller.WriteResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.ContentUris;
import android.net.Uri;

import javax.inject.Inject;
import java.util.Collections;

public class TrackSyncer implements SyncStrategy {

    private final ApiClient apiClient;
    private final StoreTracksCommand storeTracksCommand;

    @Inject
    public TrackSyncer(ApiClient apiClient, StoreTracksCommand storeTracksCommand) {
        this.apiClient = apiClient;
        this.storeTracksCommand = storeTracksCommand;
    }

    @NotNull
    @Override
    public ApiSyncResult syncContent(@Deprecated Uri uri, @Nullable String action) throws Exception {
        final Urn trackUrn = Urn.forTrack(ContentUris.parseId(uri));
        final ApiRequest request = ApiRequest.get(ApiEndpoints.TRACKS.path(trackUrn)).forPrivateApi(1).build();
        final ApiTrack track = apiClient.fetchMappedResponse(request, ApiTrack.class);

        final WriteResult writeResult = storeTracksCommand.call(Collections.singleton(track));
        if (writeResult.success()) {
            return ApiSyncResult.fromSuccessfulChange(uri);
        }
        return ApiSyncResult.fromClientError(uri);
    }
}
