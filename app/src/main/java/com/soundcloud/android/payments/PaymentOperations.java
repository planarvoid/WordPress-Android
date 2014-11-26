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
import rx.Scheduler;
import rx.Statement;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;

import android.app.Activity;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

class PaymentOperations {

    private static final int API_VERSION = 1;
    private static final int VERIFY_THROTTLE_SECONDS = 2;

    private final Scheduler scheduler;
    private final ApiScheduler api;
    private final BillingService playBilling;
    private final PaymentStorage paymentStorage;

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
    PaymentOperations(ApiScheduler api, BillingService playBilling, PaymentStorage paymentStorage) {
        this(ScSchedulers.API_SCHEDULER, api, playBilling, paymentStorage);
    }

    PaymentOperations(Scheduler scheduler, ApiScheduler api, BillingService playBilling, PaymentStorage paymentStorage) {
        this.scheduler = scheduler;
        this.api = api;
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
                .subscribeOn(scheduler)
                .flatMap(verifyPendingSubscription)
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<ProductStatus> queryProduct() {
        return getSubscriptionId()
                .flatMap(productToResult)
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<String> purchase(final String id) {
        final ApiRequest<CheckoutStarted> request = ApiRequest.Builder.<CheckoutStarted>post(ApiEndpoints.CHECKOUT.path())
                        .forPrivateApi(API_VERSION)
                        .withContent(new StartCheckout(id))
                        .forResource(CheckoutStarted.class)
                        .build();
        return api.mappedResponse(request)
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
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Observable<PurchaseStatus> update(final Payload payload) {
        return api.response(buildUpdateRequest(UpdateCheckout.fromSuccess(payload)))
                .map(TO_STATUS);
    }

    private Observable<PurchaseStatus> pollStatus() {
        final PollingState pollingState = new PollingState();
        return Statement.doWhile(delayGetStatus(), pollingState.shouldContinue())
                .doOnNext(new Action1<PurchaseStatus>() {
                    @Override
                    public void call(PurchaseStatus purchaseStatus) {
                        if (purchaseStatus.isPending()) {
                            pollingState.increment();
                        } else {
                            pollingState.resultObtained();
                        }
                    }
                })
                .filter(IGNORE_PENDING)
                .defaultIfEmpty(PurchaseStatus.VERIFY_TIMEOUT);
    }

    private Observable<PurchaseStatus> delayGetStatus() {
        return Observable.timer(VERIFY_THROTTLE_SECONDS, TimeUnit.SECONDS, scheduler)
                .flatMap(new Func1<Long, Observable<PurchaseStatus>>() {
                    @Override
                    public Observable<PurchaseStatus> call(Long time) {
                        return getStatus();
                    }
                });
    }

    private Observable<PurchaseStatus> getStatus() {
        final ApiRequest<CheckoutUpdated> request =
                ApiRequest.Builder.<CheckoutUpdated>get(ApiEndpoints.CHECKOUT_URN.path(paymentStorage.getCheckoutToken()))
                .forPrivateApi(API_VERSION)
                .forResource(CheckoutUpdated.class)
                .build();
        return api.mappedResponse(request)
                .map(CheckoutUpdated.TO_STATUS);
    }

    public Observable<ApiResponse> cancel(final String reason) {
        return api.response(buildUpdateRequest(UpdateCheckout.fromFailure(reason)));
    }

    private ApiRequest buildUpdateRequest(UpdateCheckout update) {
        return ApiRequest.Builder.post(ApiEndpoints.CHECKOUT_URN.path(paymentStorage.getCheckoutToken()))
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
        final ApiRequest<AvailableProducts> request =
                ApiRequest.Builder.<AvailableProducts>get(ApiEndpoints.PRODUCTS.path())
                        .forPrivateApi(API_VERSION)
                        .forResource(AvailableProducts.class)
                        .build();
        return api.mappedResponse(request);
    }

    private static class PollingState {

        private static final int MAX_RETRIES = 3;

        private boolean resultObtained = false;
        private int requestCount = 0;

        public Func0<Boolean> shouldContinue() {
            return new Func0<Boolean>() {
                @Override
                public Boolean call() {
                    return requestCount <= MAX_RETRIES && !resultObtained;
                }
            };
        }

        public void resultObtained() {
            resultObtained = true;
        }

        public void increment() {
            requestCount++;
        }

    }

}
