package com.soundcloud.android.sync.commands;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.BulkFetchCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.reflect.TypeToken;

import android.support.annotation.VisibleForTesting;
import android.support.v4.util.ArrayMap;

import javax.inject.Inject;
import java.util.List;

public class FetchUsersCommand extends BulkFetchCommand<ApiUser> {

    @Inject
    public FetchUsersCommand(ApiClient apiClient) {
        super(apiClient);
    }

    @VisibleForTesting
    FetchUsersCommand(ApiClient apiClient, int pageSize) {
        super(apiClient, pageSize);
    }

    @Override
    protected ApiRequest buildRequest(List<Urn> urns) {
        final ArrayMap<String, Object> body = new ArrayMap<>(1);
        body.put("urns", Urns.toString(urns));

        return ApiRequest.post(ApiEndpoints.USERS_FETCH.path())
                         .forPrivateApi()
                         .withContent(body)
                         .build();
    }

    @Override
    protected TypeToken<? extends Iterable<ApiUser>> provideResourceType() {
        return new TypeToken<ModelCollection<ApiUser>>() {
        };
    }
}
