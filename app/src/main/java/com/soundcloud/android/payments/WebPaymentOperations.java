package com.soundcloud.android.payments;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.java.reflect.TypeToken;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;

public class WebPaymentOperations {

    private static final TypeToken<ModelCollection<WebProduct>> PRODUCT_COLLECTION_TOKEN = new TypeToken<ModelCollection<WebProduct>>() {};

    private static final Func1<ModelCollection<WebProduct>, AvailableWebProducts> TO_AVAILABLE_PRODUCTS = webProducts -> new AvailableWebProducts(webProducts.getCollection());

    private final ApiClientRx apiClientRx;
    private final Scheduler scheduler;

    @Inject
    public WebPaymentOperations(ApiClientRx apiClientRx,
                                @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.apiClientRx = apiClientRx;
        this.scheduler = scheduler;
    }

    Observable<AvailableWebProducts> products() {
        return apiClientRx.mappedResponse(webProductRequest(), PRODUCT_COLLECTION_TOKEN)
                .subscribeOn(scheduler)
                .map(TO_AVAILABLE_PRODUCTS);
    }

    private ApiRequest webProductRequest() {
        return ApiRequest
                .get(ApiEndpoints.WEB_PRODUCTS.path())
                .forPrivateApi()
                .build();
    }

}
