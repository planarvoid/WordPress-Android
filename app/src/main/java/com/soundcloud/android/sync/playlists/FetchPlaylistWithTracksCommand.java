package com.soundcloud.android.sync.playlists;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.commands.LegacyCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.reflect.TypeToken;

import javax.inject.Inject;

public class FetchPlaylistWithTracksCommand extends LegacyCommand<Urn, ApiPlaylistWithTracks, FetchPlaylistWithTracksCommand> {

    private final ApiClient apiClient;

    @Inject
    public FetchPlaylistWithTracksCommand(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiPlaylistWithTracks call() throws Exception {
        final ApiRequest request =
                ApiRequest.get(ApiEndpoints.PLAYLIST_WITH_TRACKS.path(input))
                        .forPrivateApi(1)
                        .build();

        return apiClient.fetchMappedResponse(request, new TypeToken<ApiPlaylistWithTracks>() {
        });
    }
}
