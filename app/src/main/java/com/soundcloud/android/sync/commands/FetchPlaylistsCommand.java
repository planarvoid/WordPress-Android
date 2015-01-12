package com.soundcloud.android.sync.commands;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.commands.ApiResourceCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.ApiPlaylistCollection;

import android.support.v4.util.ArrayMap;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class FetchPlaylistsCommand extends ApiResourceCommand<List<Urn>, ApiPlaylistCollection> {

    @Inject
    public FetchPlaylistsCommand(ApiClient apiClient) {
        super(apiClient);
    }

    @Override
    protected ApiRequest<ApiPlaylistCollection> buildRequest() {
        final ArrayList<String> urnStrings = new ArrayList<>(input.size());
        for (Urn urn : input) {
            urnStrings.add(urn.toString());
        }

        final ArrayMap<String, Object> body = new ArrayMap<>(1);
        body.put("urns", urnStrings);

        return ApiRequest.Builder.<ApiPlaylistCollection>post(ApiEndpoints.PLAYLISTS_FETCH.path())
                .forPrivateApi(1)
                .forResource(ApiPlaylistCollection.class)
                .withContent(body)
                .build();
    }
}
