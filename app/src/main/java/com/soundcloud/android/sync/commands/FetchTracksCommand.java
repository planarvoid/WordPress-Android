package com.soundcloud.android.sync.commands;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.BulkFetchCommand;
import com.soundcloud.android.model.Urn;

import android.support.v4.util.ArrayMap;

import javax.inject.Inject;
import java.util.ArrayList;

public class FetchTracksCommand extends BulkFetchCommand<ApiTrack> {

    @Inject
    public FetchTracksCommand(ApiClient apiClient) {
        super(apiClient);
    }

    @Override
    protected ApiRequest<ModelCollection<ApiTrack>> buildRequest() {
        final ArrayList<String> urnStrings = new ArrayList<>(input.size());
        for (Urn urn : input) {
            urnStrings.add(urn.toString());
        }

        final ArrayMap<String, Object> body = new ArrayMap<>(1);
        body.put("urns", urnStrings);

        return ApiRequest.Builder.<ModelCollection<ApiTrack>>post(ApiEndpoints.TRACKS_FETCH.path())
                .forPrivateApi(1)
                .forResource(new TypeToken<ModelCollection<ApiTrack>>() {
                })
                .withContent(body)
                .build();
    }
}
