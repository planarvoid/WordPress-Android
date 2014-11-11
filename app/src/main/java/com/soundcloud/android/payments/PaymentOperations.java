package com.soundcloud.android.payments;

import static com.soundcloud.android.payments.AvailableProducts.Product;

import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.ApiScheduler;
import com.soundcloud.android.payments.googleplay.BillingService;
import com.soundcloud.android.payments.googleplay.Payload;
import com.soundcloud.android.payments.googleplay.SubscriptionStatus;
import com.soundcloud.android.rx.ScSchedulers;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;

import android.app.Activity;

import javax.inject.Inject;

class PaymentOperations {

    private final ApiScheduler apiScheduler;
    private final BillingService playBilling;
    private final PaymentStorage paymentStorage;

    private static final Func1<ApiResponse, PurchaseStatus> TO_PURCHASE_STATUS = new Func1<ApiResponse, PurchaseStatus>() {
        @Override
        public PurchaseStatus call(ApiResponse apiResponse) {
            return apiResponse.isSuccess()
                    ? PurchaseStatus.VERIFYING
                    : PurchaseStatus.FAILURE;
        }
    };

    private final Func1<SubscriptionStatus, Observable<PurchaseStatus>> verifyPendingSubscription = new Func1<SubscriptionStatus, Observable<PurchaseStatus>>() {
        @Override
        public Observable<PurchaseStatus> call(SubscriptionStatus subscriptionStatus) {
            if (subscriptionStatus.isSubscribed()) {
                paymentStorage.setCheckoutToken(subscriptionStatus.getToken());
                return verify(subscriptionStatus.getPayload());
            }
            return Observable.just(PurchaseStatus.NONE);
        }
    };

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
    PaymentOperations(ApiScheduler apiScheduler, BillingService playBilling, PaymentStorage paymentStorage) {
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

    public Observable<PurchaseStatus> queryStatus() {
        return playBilling.getStatus()
                .subscribeOn(ScSchedulers.API_SCHEDULER)
                .flatMap(verifyPendingSubscription)
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<ProductStatus> queryProduct() {
        return getSubscriptionId()
                .flatMap(productToResult)
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<String> purchase(final String id) {
        final ApiRequest<CheckoutStarted> request =
                ApiRequest.Builder.<CheckoutStarted>post(ApiEndpoints.CHECKOUT.path())
                        .forPrivateApi(1)
                        .withContent(new StartCheckout(id))
                        .forResource(CheckoutStarted.class)
                        .build();
        return apiScheduler.mappedResponse(request)
                .map(CheckoutStarted.TOKEN)
                .doOnNext(saveToken)
                .doOnNext(launchPaymentFlow(id))
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Action1<String> launchPaymentFlow(final String id) {
        return new Action1<String>() {
            @Override
            public void call(String token) {
                playBilling.startPurchase(id, token);
            }
        };
    }

    public Observable<PurchaseStatus> verify(final Payload payload) {
        return apiScheduler.response(buildUpdateRequest(UpdateCheckout.fromSuccess(payload)))
                .map(TO_PURCHASE_STATUS);
    }

    public Observable<ApiResponse> cancel(final String reason) {
        return apiScheduler.response(buildUpdateRequest(UpdateCheckout.fromFailure(reason)));
    }

    private ApiRequest buildUpdateRequest(UpdateCheckout update) {
        return ApiRequest.Builder.post(ApiEndpoints.CHECKOUT_URN.path(paymentStorage.getCheckoutToken()))
                .forPrivateApi(1)
                .withContent(update)
                .build();
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
