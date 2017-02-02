package com.soundcloud.android.discovery.newforyou;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.java.reflect.TypeToken;

import javax.inject.Inject;
import java.util.concurrent.Callable;

public class NewForYouSyncer implements Callable<Boolean> {

    private ApiClient apiClient;
    private NewForYouStorage newForYouStorage;

    @Inject
    public NewForYouSyncer(ApiClient apiClient,
                           NewForYouStorage newForYouStorage) {
        this.apiClient = apiClient;
        this.newForYouStorage = newForYouStorage;
    }

    @Override
    public Boolean call() throws Exception {
        ApiRequest apiRequest = ApiRequest.get(ApiEndpoints.NEW_FOR_YOU.path())
                                          .forPrivateApi()
                                          .build();

        ApiNewForYou apiNewForYou = apiClient.fetchMappedResponse(apiRequest, TypeToken.of(ApiNewForYou.class));
        return newForYouStorage.storeNewForYou(apiNewForYou);
    }
}
