package com.soundcloud.android.payments;

import static com.soundcloud.android.payments.AvailableProducts.Product;

import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiScheduler;
import com.soundcloud.android.payments.googleplay.PlayBillingResult;
import com.soundcloud.android.payments.googleplay.PlayBillingService;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.utils.Log;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;

import android.app.Activity;

import javax.inject.Inject;

class PaymentOperations {

    private static final String TAG = "PaymentOps";

    private final ApiScheduler apiScheduler;
    private final PlayBillingService playBilling;
    private final PaymentStorage paymentStorage;

    private final Func1<Product, Observable<ProductStatus>> productToResult = new Func1<Product, Observable<ProductStatus>>() {
        @Override
        public Observable<ProductStatus> call(Product product) {
            return product.isEmpty()
                    ? Observable.just(ProductStatus.fromNoProduct())
                    : queryProduct(product.id).map(ProductStatus.SUCCESS);
        }
    };

    private final Action1<String> saveToken = new Action1<String>() {
        @Override
        public void call(String checkoutToken) {
            paymentStorage.setCheckoutToken(checkoutToken);
        }
    };

    @Inject
    PaymentOperations(ApiScheduler apiScheduler, PlayBillingService playBilling, PaymentStorage paymentStorage) {
        this.apiScheduler = apiScheduler;
        this.playBilling = playBilling;
        this.paymentStorage = paymentStorage;
    }

    public Observable<ConnectionStatus> connect(Activity activity) {
        return playBilling.openConnection(activity);
    }

    public void disconnect() {
        playBilling.closeConnection();
    }

    public Observable<ProductStatus> queryProduct() {
        return getSubscriptionId()
                .flatMap(productToResult)
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<String> purchase(final String id) {
        final ApiRequest<CheckoutStart> request =
                ApiRequest.Builder.<CheckoutStart>post(ApiEndpoints.CHECKOUT.path())
                        .forPrivateApi(1)
                        .forResource(CheckoutStart.class)
                        .addQueryParameters("product_id", id)
                        .build();
        return apiScheduler.mappedResponse(request)
                .map(CheckoutStart.TOKEN)
                .doOnNext(saveToken)
                .doOnNext(launchPaymentFlow(id));
    }

    private Action1<String> launchPaymentFlow(final String id) {
        return new Action1<String>() {
            @Override
            public void call(String token) {
                playBilling.startPurchase(id, token);
            }
        };
    }

    public Observable<PurchaseStatus> verify(final PlayBillingResult result) {
        Log.e(TAG, "Verify: " + paymentStorage.getCheckoutToken());
        // TODO: POST /checkout/:token with purchase JSON & signature
        return Observable.just(PurchaseStatus.VERIFYING);
    }

    public Observable<Void> cancel(final PlayBillingResult result) {
        Log.e(TAG, "Cancel: " + paymentStorage.getCheckoutToken());
        // TODO: POST /checkout/:token with updated status
        return Observable.just(null);
    }

    private Observable<ProductDetails> queryProduct(String id) {
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
        return apiScheduler.mappedResponse(request);
    }

}
