package com.soundcloud.android.sync.commands;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.legacy.model.CollectionHolder;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.commands.BulkFetchCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.users.UserRecord;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.reflect.TypeToken;

import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FetchUsersCommand extends BulkFetchCommand<PublicApiUser, UserRecord> {

    @Inject
    public FetchUsersCommand(ApiClient apiClient) {
        super(apiClient);
    }

    @VisibleForTesting
    FetchUsersCommand(ApiClient apiClient, int pageSize) {
        super(apiClient, pageSize);
    }

    @Override
    protected Collection<UserRecord> transformResults(Collection<PublicApiUser> results) {
        return new ArrayList<>(results);
    }

    @Override
    protected ApiRequest buildRequest(List<Urn> urns) {
        return ApiRequest.get(ApiEndpoints.LEGACY_USERS.path())
                         .forPublicApi()
                         .addQueryParam("ids", Urns.toJoinedIds(urns, ","))
                         .addQueryParam("linked_partitioning", "1")
                         .build();
    }

    @Override
    protected TypeToken<? extends Iterable<PublicApiUser>> provideResourceType() {
        return new TypeToken<CollectionHolder<PublicApiUser>>() {
        };
    }
}
