package com.soundcloud.android.policies;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.MoreCollections;
import com.soundcloud.java.reflect.TypeToken;
import com.soundcloud.propeller.WriteResult;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class UpdatePoliciesCommand extends Command<Collection<Urn>, Collection<ApiPolicyInfo>> {

    static final int BATCH_SIZE = 300;

    private static final String TAG = "PolicyUpdater";
    private static final TypeToken<ModelCollection<ApiPolicyInfo>> TYPE_TOKEN =
            new TypeToken<ModelCollection<ApiPolicyInfo>>() {
            };

    private final ApiClient apiClient;
    private final StorePoliciesCommand storePolicies;

    @Inject
    UpdatePoliciesCommand(ApiClient apiClient,
                          StorePoliciesCommand storePolicies) {
        this.apiClient = apiClient;
        this.storePolicies = storePolicies;
    }

    @Override
    public Collection<ApiPolicyInfo> call(Collection<Urn> trackUrns) throws PolicyUpdateFailure {
        try {
            final List<ApiPolicyInfo> updatedPolicies = new ArrayList<>(trackUrns.size());
            for (List<Urn> urnBatch : Iterables.partition(trackUrns, BATCH_SIZE)) {
                Log.d(TAG, "Fetching policy batch: " + urnBatch.size());
                final ModelCollection<ApiPolicyInfo> apiPolicyInfos =
                        apiClient.fetchMappedResponse(buildApiRequest(urnBatch), TYPE_TOKEN);

                Log.d(TAG, "Writing policy batch");
                final WriteResult result = storePolicies.call(apiPolicyInfos);
                if (!result.success()) {
                    throw result.getFailure();
                }
                Log.d(TAG, "OK!");
                updatedPolicies.addAll(apiPolicyInfos.getCollection());
            }

            return updatedPolicies;
        } catch (Exception e) {
            throw new PolicyUpdateFailure(e);
        }
    }

    private ApiRequest buildApiRequest(Collection<Urn> trackUrns) {
        return ApiRequest.post(ApiEndpoints.POLICIES.path())
                .withContent(MoreCollections.transform(trackUrns, Urns.toStringFunc()))
                .forPrivateApi(1)
                .build();
    }

}
