package com.soundcloud.android.discovery;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.utils.LocaleFormatter;
import com.soundcloud.java.reflect.TypeToken;

import javax.inject.Inject;
import java.util.concurrent.Callable;

class DiscoveryCardSyncer implements Callable<Boolean> {

    private static final String PARAM_LOCALE = "locale";
    private final LocaleFormatter localeFormatter;
    private final ApiClient apiClient;
    private final DiscoveryWritableStorage discoveryWritableStorage;

    @Inject
    DiscoveryCardSyncer(ApiClient apiClient,
                        DiscoveryWritableStorage discoveryWritableStorage,
                        LocaleFormatter localeFormatter) {
        this.apiClient = apiClient;
        this.discoveryWritableStorage = discoveryWritableStorage;
        this.localeFormatter = localeFormatter;
    }

    @Override
    public Boolean call() throws Exception {
        final ApiRequest.Builder builder = ApiRequest.get(ApiEndpoints.DISCOVERY_CARDS.path());
        localeFormatter.getLocale().ifPresent(locale -> builder.addQueryParam(PARAM_LOCALE, locale));
        final ApiRequest apiRequest = builder.forPrivateApi().build();
        final ModelCollection<ApiDiscoveryCard> apiDiscoveryCards = apiClient.fetchMappedResponse(apiRequest, new TypeToken<ModelCollection<ApiDiscoveryCard>>() {});
        discoveryWritableStorage.storeDiscoveryCards(apiDiscoveryCards);

        // we always want to say nothing has changed. We reset the backoff in DiscoveryPresenter
        return false;
    }
}
