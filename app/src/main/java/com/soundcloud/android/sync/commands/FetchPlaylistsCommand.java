package com.soundcloud.android.sync.commands;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.BulkFetchCommand;
import com.soundcloud.android.model.Urn;

import android.support.v4.util.ArrayMap;

import javax.inject.Inject;
import java.util.ArrayList;

public class FetchPlaylistsCommand extends BulkFetchCommand<ApiPlaylist> {

    @Inject
    public FetchPlaylistsCommand(ApiClient apiClient) {
        super(apiClient);
    }

    @Override
    protected ApiRequest<ModelCollection<ApiPlaylist>> buildRequest() {
        final ArrayList<String> urnStrings = new ArrayList<>(input.size());
        for (Urn urn : input) {
            urnStrings.add(urn.toString());
        }

        final ArrayMap<String, Object> body = new ArrayMap<>(1);
        body.put("urns", urnStrings);

        return ApiRequest.Builder.<ModelCollection<ApiPlaylist>>post(ApiEndpoints.PLAYLISTS_FETCH.path())
                .forPrivateApi(1)
                .forResource(new TypeToken<ModelCollection<ApiPlaylist>>() {
                })
                .withContent(body)
                .build();
    }
}
