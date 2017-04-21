package com.soundcloud.android.home;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.configuration.experiments.ExperimentOperations;
import com.soundcloud.android.utils.LocaleFormatter;
import com.soundcloud.java.reflect.TypeToken;

import javax.inject.Inject;
import java.util.concurrent.Callable;

class HomeSyncer implements Callable<Boolean> {

    private static final String PARAM_LOCALE = "locale";
    private static final String PARAM_EXPERIMENT = "experiment_variant";
    private final LocaleFormatter localeFormatter;
    private final ExperimentOperations experimentOperations;
    private final ApiClient apiClient;
    private final HomeStorage homeStorage;

    @Inject
    HomeSyncer(ApiClient apiClient,
                      HomeStorage homeStorage,
                      LocaleFormatter localeFormatter,
                      ExperimentOperations experimentOperations) {
        this.apiClient = apiClient;
        this.homeStorage = homeStorage;
        this.localeFormatter = localeFormatter;
        this.experimentOperations = experimentOperations;
    }

    @Override
    public Boolean call() throws Exception {
        final ApiRequest.Builder builder = ApiRequest.get(ApiEndpoints.HOME.path());
        localeFormatter.getLocale().ifPresent(locale -> builder.addQueryParam(PARAM_LOCALE, locale));
        experimentOperations.getSerializedActiveVariants().ifPresent(activeVariants -> builder.addQueryParam(PARAM_EXPERIMENT, activeVariants));
        final ApiRequest apiRequest = builder.forPrivateApi().build();
        final ModelCollection<ApiHomeCard> apiHome = apiClient.fetchMappedResponse(apiRequest, new TypeToken<ModelCollection<ApiHomeCard>>() {});
        return homeStorage.store(apiHome);
    }
}
