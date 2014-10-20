package com.soundcloud.android.payments;

import static com.soundcloud.android.payments.AvailableProducts.Product;

import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
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

    private final Func1<Product, Observable<ProductStatus>> productToResult = new Func1<Product, Observable<ProductStatus>>() {
        @Override
        public Observable<ProductStatus> call(Product product) {
            return product.isEmpty()
                    ? Observable.just(ProductStatus.fromNoProduct())
                    : queryProductDetails(product.id).map(ProductStatus.SUCCESS);
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
                .flatMap(productToResult)
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<String> buy(String id) {
        final ApiRequest<CheckoutResult> request =
                ApiRequest.Builder.<CheckoutResult>post(ApiEndpoints.CHECKOUT.path())
                        .forPrivateApi(1)
                        .forResource(CheckoutResult.class)
                        .addQueryParameters("product_id", id)
                        .build();
        return rxHttpClient.<CheckoutResult>fetchModels(request)
                .map(CheckoutResult.TOKEN)
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Observable<ProductDetails> queryProductDetails(String id) {
        return playBilling.getDetails(id)
                .subscribeOn(ScSchedulers.API_SCHEDULER);
    }

    private Observable<Product> getSubscriptionId() {
        return fetchAvailableProducts()
                .map(AvailableProducts.TO_PRODUCT);
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
