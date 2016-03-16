package com.soundcloud.android.sync.playlists;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.commands.LegacyCommand;
import com.soundcloud.android.model.Urn;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class PushPlaylistAdditionsCommand extends LegacyCommand<Collection<Urn>, Collection<Urn>, PushPlaylistAdditionsCommand> {

    private final ApiClient apiClient;

    private Urn playlistUrn;

    @Inject
    PushPlaylistAdditionsCommand(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public PushPlaylistAdditionsCommand with(Urn playlistUrn) {
        this.playlistUrn = playlistUrn;
        return this;
    }

    public Collection<Urn> call() throws Exception {
        List<Urn> successes = new ArrayList<>(input.size());
        for (Urn urn : input) {
            final ApiRequest request =
                    ApiRequest.post(ApiEndpoints.PLAYLIST_ADD_TRACK.path(playlistUrn))
                            .forPrivateApi()
                            .withContent(Collections.singletonMap("track_urn", urn.toString()))
                            .build();

            final ApiResponse apiResponse = apiClient.fetchResponse(request);
            if (apiResponse.isSuccess()) {
                successes.add(urn);
            } else {
                throwNetworkOrServerError(apiResponse);
            }
        }
        return successes;
    }

    // also used in PushPlaylistRemovalCommand. Might want to think about consolidating this on ApiResponse
    private void throwNetworkOrServerError(ApiResponse apiResponse) throws ApiRequestException {
        final ApiRequestException failure = apiResponse.getFailure();
        if (failure != null && (failure.isNetworkError() || apiResponse.getStatusCode() >= 500)) {
            throw failure;
        }
    }
}
