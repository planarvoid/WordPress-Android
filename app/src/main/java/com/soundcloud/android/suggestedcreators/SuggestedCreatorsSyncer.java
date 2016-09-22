package com.soundcloud.android.suggestedcreators;


import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.sync.suggestedCreators.ApiSuggestedCreators;
import com.soundcloud.java.reflect.TypeToken;
import com.soundcloud.propeller.WriteResult;

import javax.inject.Inject;
import java.util.concurrent.Callable;

class SuggestedCreatorsSyncer implements Callable<Boolean> {

    private final ApiClient apiClient;
    private final StoreSuggestedCreatorsCommand storeSuggestedCreatorsCommand;

    @Inject
    public SuggestedCreatorsSyncer(ApiClient apiClient, StoreSuggestedCreatorsCommand storeSuggestedCreatorsCommand) {
        this.apiClient = apiClient;
        this.storeSuggestedCreatorsCommand = storeSuggestedCreatorsCommand;
    }

    @Override
    public Boolean call() throws Exception {
        final ApiRequest request = ApiRequest.get(ApiEndpoints.SUGGESTED_CREATORS.path()).forPrivateApi().build();
        final ApiSuggestedCreators apiSuggestedCreators = apiClient.fetchMappedResponse(request, TypeToken.of(ApiSuggestedCreators.class));
        final WriteResult writeResult = storeSuggestedCreatorsCommand.call(apiSuggestedCreators);
        return writeResult.success();
    }
}
