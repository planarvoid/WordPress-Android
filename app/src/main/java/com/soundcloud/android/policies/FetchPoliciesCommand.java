package com.soundcloud.android.policies;

import com.google.common.collect.Collections2;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.GuavaFunctions;

import javax.inject.Inject;
import java.util.Collection;

class FetchPoliciesCommand extends Command<Collection<Urn>, Collection<PolicyInfo>, FetchPoliciesCommand>{

    private final ApiClient apiClient;

    @Inject
    FetchPoliciesCommand(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public Collection<PolicyInfo> call() throws Exception {
        final ApiRequest<ModelCollection<PolicyInfo>> request =
                ApiRequest.Builder.<ModelCollection<PolicyInfo>>post(ApiEndpoints.POLICIES.path())
                .withContent(Collections2.transform(input, GuavaFunctions.urnToString()))
                .forPrivateApi(1)
                .forResource(new TypeToken<ModelCollection<PolicyInfo>>() {})
                .build();
        return apiClient.fetchMappedResponse(request).getCollection();
    }

}
