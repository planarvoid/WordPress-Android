package com.soundcloud.android.payments;

import static com.soundcloud.android.payments.AvailableProducts.Product;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.payments.googleplay.BillingService;
import com.soundcloud.android.payments.googleplay.Payload;
import com.soundcloud.android.payments.googleplay.SubscriptionStatus;
import com.soundcloud.android.rx.ScSchedulers;
import rx.Observable;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

import android.app.Activity;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

class PaymentOperations {

    private static final int API_VERSION = 1;
    private static final int VERIFY_THROTTLE_SECONDS = 2;

    private final Scheduler scheduler;
    private final ApiClientRx api;
    private final BillingService playBilling;
    private final TokenStorage tokenStorage;

    private static final Func1<ApiResponse, PurchaseStatus> TO_STATUS = new Func1<ApiResponse, PurchaseStatus>() {
        @Override
        public PurchaseStatus call(ApiResponse apiResponse) {
            return apiResponse.isSuccess()
                    ? PurchaseStatus.PENDING
                    : PurchaseStatus.UPDATE_FAIL;
        }
    };

    private static final Func1<PurchaseStatus, Boolean> IGNORE_PENDING = new Func1<PurchaseStatus, Boolean>() {
        @Override
        public Boolean call(PurchaseStatus purchaseStatus) {
            return !purchaseStatus.isPending();
        }
    };

    private final Func1<SubscriptionStatus, Observable<PurchaseStatus>> verifyPendingSubscription = new Func1<SubscriptionStatus, Observable<PurchaseStatus>>() {
        @Override
        public Observable<PurchaseStatus> call(SubscriptionStatus subscriptionStatus) {
            if (subscriptionStatus.isSubscribed()) {
                tokenStorage.setCheckoutToken(subscriptionStatus.getToken());
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
            tokenStorage.setCheckoutToken(checkoutToken);
        }
    };

    private final Action0 clearToken = new Action0() {
        @Override
        public void call() {
            tokenStorage.clear();
        }
    };

    @Inject
    PaymentOperations(ApiClientRx api, BillingService playBilling, TokenStorage tokenStorage) {
        this(ScSchedulers.HIGH_PRIO_SCHEDULER, api, playBilling, tokenStorage);
    }

    PaymentOperations(Scheduler scheduler, ApiClientRx api, BillingService playBilling, TokenStorage tokenStorage) {
        this.scheduler = scheduler;
        this.api = api;
        this.playBilling = playBilling;
        this.tokenStorage = tokenStorage;
    }

    Observable<ConnectionStatus> connect(Activity activity) {
        return playBilling.openConnection(activity);
    }

    void disconnect() {
        playBilling.closeConnection();
    }

    Observable<PurchaseStatus> queryStatus() {
        return playBilling.getStatus()
                .subscribeOn(scheduler)
                .flatMap(verifyPendingSubscription)
                .observeOn(AndroidSchedulers.mainThread());
    }

    Observable<ProductStatus> queryProduct() {
        return getSubscriptionId()
                .flatMap(productToResult)
                .observeOn(AndroidSchedulers.mainThread());
    }

    Observable<String> purchase(final String id) {
        final ApiRequest request = ApiRequest.post(ApiEndpoints.CHECKOUT.path())
                        .forPrivateApi(API_VERSION)
                        .withContent(new StartCheckout(id))
                        .build();
        return api.mappedResponse(request, CheckoutStarted.class)
                .subscribeOn(scheduler)
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

    Observable<PurchaseStatus> verify(final Payload payload) {
        return update(payload)
                .flatMap(new Func1<PurchaseStatus, Observable<PurchaseStatus>>() {
                    @Override
                    public Observable<PurchaseStatus> call(PurchaseStatus purchaseStatus) {
                        if (purchaseStatus.isPending()) {
                            return pollStatus();
                        }
                        return Observable.just(PurchaseStatus.UPDATE_FAIL);
                    }
                })
                .doOnCompleted(clearToken)
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Observable<PurchaseStatus> update(final Payload payload) {
        return api.response(buildUpdateRequest(UpdateCheckout.fromSuccess(payload)))
                .subscribeOn(scheduler)
                .map(TO_STATUS);
    }

    private Observable<PurchaseStatus> pollStatus() {
        return Observable.interval(VERIFY_THROTTLE_SECONDS, TimeUnit.SECONDS, scheduler)
                .take(4)
                .flatMap(new Func1<Long, Observable<PurchaseStatus>>() {
                    @Override
                    public Observable<PurchaseStatus> call(Long tick) {
                        return getStatus();
                    }
                })
                .filter(IGNORE_PENDING)
                .firstOrDefault(PurchaseStatus.VERIFY_TIMEOUT);
    }

    private Observable<PurchaseStatus> getStatus() {
        final ApiRequest request =
                ApiRequest.get(ApiEndpoints.CHECKOUT_URN.path(tokenStorage.getCheckoutToken()))
                .forPrivateApi(API_VERSION)
                .build();
        return api.mappedResponse(request, CheckoutUpdated.class)
                .subscribeOn(scheduler)
                .map(CheckoutUpdated.TO_STATUS);
    }

    public Observable<ApiResponse> cancel(final String reason) {
        return api.response(buildUpdateRequest(UpdateCheckout.fromFailure(reason)))
                .subscribeOn(scheduler)
                .doOnCompleted(clearToken);
    }

    private ApiRequest buildUpdateRequest(UpdateCheckout update) {
        return ApiRequest.post(ApiEndpoints.CHECKOUT_URN.path(tokenStorage.getCheckoutToken()))
                .forPrivateApi(API_VERSION)
                .withContent(update)
                .build();
    }

    private Observable<ProductDetails> queryProduct(String id) {
        return playBilling.getDetails(id)
                .subscribeOn(scheduler);
    }

    private Observable<Product> getSubscriptionId() {
        return fetchAvailableProducts()
                .map(AvailableProducts.TO_PRODUCT);
    }

    private Observable<AvailableProducts> fetchAvailableProducts() {
        final ApiRequest request =
                ApiRequest.get(ApiEndpoints.PRODUCTS.path())
                        .forPrivateApi(API_VERSION)
                        .build();
        return api.mappedResponse(request, AvailableProducts.class).subscribeOn(scheduler);
    }

}
