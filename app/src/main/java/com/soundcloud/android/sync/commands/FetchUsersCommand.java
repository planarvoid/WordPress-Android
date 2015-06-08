package com.soundcloud.android.sync.commands;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.legacy.PublicApiWrapper;
import com.soundcloud.android.api.legacy.model.CollectionHolder;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.commands.BulkFetchCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.CollectionUtils;

import javax.inject.Inject;
import java.util.List;

public class FetchUsersCommand extends BulkFetchCommand<PublicApiUser> {

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
        return ApiRequest.get(ApiEndpoints.LEGACY_USERS.path())
                .forPublicApi()
                .addQueryParam("ids", CollectionUtils.urnsToJoinedIds(urns, ","))
                .addQueryParam(PublicApiWrapper.LINKED_PARTITIONING, "1")
                .build();
    }

    @Override
    protected TypeToken<? extends Iterable<PublicApiUser>> provideResourceType() {
        return new TypeToken<CollectionHolder<PublicApiUser>>() {};
    }
}
