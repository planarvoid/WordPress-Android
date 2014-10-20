package com.soundcloud.android.payments;

import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.payments.googleplay.PlayBillingService;
import com.soundcloud.android.rx.ScSchedulers;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

import android.app.Activity;

import javax.inject.Inject;

class PaymentOperations {

    private final RxHttpClient rxHttpClient;
    private final PlayBillingService playBilling;

    private final Func1<String, Observable<ProductStatus>> idToProduct = new Func1<String, Observable<ProductStatus>>() {
        @Override
        public Observable<ProductStatus> call(String id) {
            return id == null ? Observable.just(ProductStatus.fromNoProduct())
                    : queryProductDetails(id).map(ProductStatus.SUCCESS);
        }
    };

    private static final Func1<AvailableProducts, String> PRODUCTS_TO_ID = new Func1<AvailableProducts, String>() {
        @Override
        public String call(AvailableProducts availableProducts) {
            if (availableProducts.isEmpty()) {
                return null;
            }
            return availableProducts.products.get(0).id;
        }
    };

    @Inject
    PaymentOperations(RxHttpClient rxHttpClient, PlayBillingService playBilling) {
        this.rxHttpClient = rxHttpClient;
        this.playBilling = playBilling;
    }

    public Observable<ConnectionStatus> connect(Activity activity) {
        return playBilling.openConnection(activity);
    }

    public void disconnect() {
        playBilling.closeConnection();
    }

    public Observable<ProductStatus> queryProductDetails() {
        return getSubscriptionId()
                .flatMap(idToProduct)
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Observable<ProductDetails> queryProductDetails(String id) {
        return playBilling.getDetails(id)
                .subscribeOn(ScSchedulers.API_SCHEDULER);
    }

    private Observable<String> getSubscriptionId() {
        return fetchAvailableProducts()
                .map(PRODUCTS_TO_ID);
    }

    private Observable<AvailableProducts> fetchAvailableProducts() {
        final ApiRequest<AvailableProducts> request =
                ApiRequest.Builder.<AvailableProducts>get(ApiEndpoints.PRODUCTS.path())
                        .forPrivateApi(1)
                        .forResource(AvailableProducts.class)
                        .build();
        return rxHttpClient.fetchModels(request);
    }

}
