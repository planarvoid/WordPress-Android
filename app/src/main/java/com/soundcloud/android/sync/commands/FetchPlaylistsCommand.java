package com.soundcloud.android.sync.commands;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.BulkFetchCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.reflect.TypeToken;

import android.support.annotation.VisibleForTesting;
import android.support.v4.util.ArrayMap;

import javax.inject.Inject;
import java.util.List;

public class FetchPlaylistsCommand extends BulkFetchCommand<ApiPlaylist> {

    @Inject
    public FetchPlaylistsCommand(ApiClient apiClient) {
        super(apiClient);
    }

    @VisibleForTesting
    FetchPlaylistsCommand(ApiClient apiClient, int pageSize) {
        super(apiClient, pageSize);
    }

    @Override
    protected ApiRequest buildRequest(List<Urn> urns) {
        final ArrayMap<String, Object> body = new ArrayMap<>(1);
        body.put("urns", Urns.toString(urns));

        return ApiRequest.post(ApiEndpoints.PLAYLISTS_FETCH.path())
                .forPrivateApi(1)
                .withContent(body)
                .build();
    }

    @Override
    protected TypeToken<ModelCollection<ApiPlaylist>> provideResourceType() {
        return new TypeToken<ModelCollection<ApiPlaylist>>() {
        };
    }
}
