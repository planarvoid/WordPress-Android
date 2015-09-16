package com.soundcloud.android.policies;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.LegacyCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.GuavaFunctions;
import com.soundcloud.java.collections.MoreCollections;
import com.soundcloud.java.reflect.TypeToken;

import javax.inject.Inject;
import java.util.Collection;

class FetchPoliciesCommand extends LegacyCommand<Collection<Urn>, Collection<ApiPolicyInfo>, FetchPoliciesCommand> {

    private final ApiClient apiClient;

    @Inject
    FetchPoliciesCommand(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public Collection<ApiPolicyInfo> call() throws Exception {
        final ApiRequest request =
                ApiRequest.post(ApiEndpoints.POLICIES.path())
                .withContent(MoreCollections.transform(input, GuavaFunctions.urnToString()))
                .forPrivateApi(1)
                .build();
        return apiClient.fetchMappedResponse(request, new TypeToken<ModelCollection<ApiPolicyInfo>>() {}).getCollection();
    }
}
