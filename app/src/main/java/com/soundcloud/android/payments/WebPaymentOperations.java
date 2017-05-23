package com.soundcloud.android.payments;

import static com.soundcloud.android.ApplicationModule.RX_HIGH_PRIORITY;

import com.soundcloud.android.api.ApiClientRxV2;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.java.reflect.TypeToken;
import io.reactivex.Scheduler;
import io.reactivex.Single;

import javax.inject.Inject;
import javax.inject.Named;

public class WebPaymentOperations {

    private static final TypeToken<ModelCollection<WebProduct>> PRODUCT_COLLECTION_TOKEN = new TypeToken<ModelCollection<WebProduct>>() {};

    private final ApiClientRxV2 apiClient;
    private final Scheduler scheduler;

    @Inject
    public WebPaymentOperations(ApiClientRxV2 apiClient,
                                @Named(RX_HIGH_PRIORITY) Scheduler scheduler) {
        this.apiClient = apiClient;
        this.scheduler = scheduler;
    }

    Single<AvailableWebProducts> products() {
        return apiClient.mappedResponse(webProductRequest(), PRODUCT_COLLECTION_TOKEN)
                        .subscribeOn(scheduler)
                        .map(response -> AvailableWebProducts.fromList(response.getCollection()));
    }

    private ApiRequest webProductRequest() {
        return ApiRequest
                .get(ApiEndpoints.WEB_PRODUCTS.path())
                .forPrivateApi()
                .build();
    }

}
