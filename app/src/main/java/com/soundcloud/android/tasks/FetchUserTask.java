package com.soundcloud.android.tasks;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.storage.LegacyUserStorage;

public class FetchUserTask extends FetchModelTask<PublicApiUser> {
    public FetchUserTask(ApiClient api) {
        super(api);
    }

    public PublicApiUser currentUser() {
        ApiRequest request = ApiRequest.get(ApiEndpoints.CURRENT_USER.path()).forPublicApi().build();
        return resolve(request);
    }

    public void execute(long userId) {
        ApiRequest request = ApiRequest.get(ApiEndpoints.LEGACY_USER.path(userId)).forPublicApi().build();
        execute(request);
    }

    @Override
    protected void persist(PublicApiUser user) {
        new LegacyUserStorage().createOrUpdate(user);
    }
}
