package com.soundcloud.android.sync.playlists;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.Urn;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class PushPlaylistAdditionsCommand extends Command<Collection<Urn>, Collection<Urn>, PushPlaylistAdditionsCommand> {

    private final ApiClient apiClient;

    private Urn playlistUrn;

    @Inject
    PushPlaylistAdditionsCommand(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public PushPlaylistAdditionsCommand with(Urn playlistUrn){
        this.playlistUrn = playlistUrn;
        return this;
    }

    public Collection<Urn> call() throws Exception {
        List<Urn> successes = new ArrayList<>(input.size());
        for (Urn urn : input){
            final ApiRequest<ApiPlaylistWithTracks> request =
                    ApiRequest.Builder.<ApiPlaylistWithTracks>post(ApiEndpoints.PLAYLIST_ADD_TRACK.path(playlistUrn))
                            .forPrivateApi(1)
                            .withContent(Collections.singletonMap("track_urn", urn))
                            .build();
            if (apiClient.fetchResponse(request).isSuccess()){
                successes.add(urn);
            }
        }
        return successes;
    }
}
