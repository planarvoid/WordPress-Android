package com.soundcloud.android.payments;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.reflect.TypeToken;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;

public class WebPaymentOperations {

    private static final String HIGH_TIER_PLAN_ID = "high_tier";

    private static final Func1<ModelCollection<WebProduct>, Optional<WebProduct>> TO_WEB_PRODUCT = new Func1<ModelCollection<WebProduct>, Optional<WebProduct>>() {
        @Override
        public Optional<WebProduct> call(ModelCollection<WebProduct> webProducts) {
            for (WebProduct product : webProducts) {
                if (product.getPlanId().equals(HIGH_TIER_PLAN_ID)) {
                    return Optional.of(product);
                }
            }
            return Optional.absent();
        }
    };

    private final ApiClientRx apiClientRx;
    private final Scheduler scheduler;

    @Inject
    public WebPaymentOperations(ApiClientRx apiClientRx,
                                @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.apiClientRx = apiClientRx;
        this.scheduler = scheduler;
    }

    Observable<Optional<WebProduct>> product() {
        final ApiRequest request = ApiRequest
                .get(ApiEndpoints.WEB_PRODUCTS.path())
                .forPrivateApi()
                .build();

        return apiClientRx.mappedResponse(request, new TypeToken<ModelCollection<WebProduct>>() {})
                          .subscribeOn(scheduler)
                          .map(TO_WEB_PRODUCT);
    }
}
