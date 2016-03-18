package com.soundcloud.android.sync.playlists;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.commands.LegacyCommand;
import com.soundcloud.android.model.Urn;

import javax.inject.Inject;
import java.util.Collection;

class PushPlaylistRemovalsCommand extends LegacyCommand<Collection<Urn>, Collection<Urn>, PushPlaylistRemovalsCommand> {

    private final ApiClient apiClient;

    private Urn playlistUrn;

    @Inject
    PushPlaylistRemovalsCommand(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public PushPlaylistRemovalsCommand with(Urn playlistUrn) {
        this.playlistUrn = playlistUrn;
        return this;
    }

    public Collection<Urn> call() throws Exception {
        for (Urn urn : input) {
            final ApiRequest request =
                    ApiRequest.<ApiPlaylistWithTracks>delete(ApiEndpoints.PLAYLIST_REMOVE_TRACK.path(playlistUrn, urn))
                            .forPrivateApi()
                            .build();
            throwNetworkOrServerError(apiClient.fetchResponse(request));
        }
        return input;
    }

    private void throwNetworkOrServerError(ApiResponse apiResponse) throws ApiRequestException {
        final ApiRequestException failure = apiResponse.getFailure();
        if (failure != null && (failure.isNetworkError() || apiResponse.getStatusCode() >= 500)) {
            throw failure;
        }
    }

}
