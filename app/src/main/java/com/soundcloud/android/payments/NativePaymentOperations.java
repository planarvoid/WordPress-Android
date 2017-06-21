package com.soundcloud.android.payments;

import static com.soundcloud.android.ApplicationModule.RX_HIGH_PRIORITY;
import static com.soundcloud.android.payments.AvailableProducts.Product;

import com.soundcloud.android.api.ApiClientRxV2;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.payments.googleplay.BillingService;
import com.soundcloud.android.payments.googleplay.Payload;
import com.soundcloud.android.payments.googleplay.SubscriptionStatus;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;

import android.app.Activity;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.TimeUnit;

class NativePaymentOperations {

    private static final int VERIFY_THROTTLE_SECONDS = 2;

    private final Scheduler scheduler;
    private final ApiClientRxV2 apiClient;
    private final BillingService playBilling;
    private final TokenStorage tokenStorage;

    private final Function<SubscriptionStatus, Single<PurchaseStatus>> verifyPendingSubscription = new Function<SubscriptionStatus, Single<PurchaseStatus>>() {
        @Override
        public Single<PurchaseStatus> apply(@NonNull SubscriptionStatus subscriptionStatus) throws Exception {
            if (subscriptionStatus.isSubscribed()) {
                tokenStorage.setCheckoutToken(subscriptionStatus.getToken());
                return verify(subscriptionStatus.getPayload());
            }
            return Single.just(PurchaseStatus.NONE);
        }
    };

    @Inject
    NativePaymentOperations(@Named(RX_HIGH_PRIORITY) Scheduler scheduler,
                            ApiClientRxV2 apiClient,
                            BillingService playBilling,
                            TokenStorage tokenStorage) {
        this.scheduler = scheduler;
        this.apiClient = apiClient;
        this.playBilling = playBilling;
        this.tokenStorage = tokenStorage;
    }

    Observable<ConnectionStatus> connect(Activity activity) {
        return playBilling.openConnection(activity);
    }

    void disconnect() {
        playBilling.closeConnection();
    }

    Single<PurchaseStatus> queryStatus() {
        return playBilling.getStatus()
                          .subscribeOn(scheduler)
                          .flatMap(verifyPendingSubscription)
                          .observeOn(AndroidSchedulers.mainThread());
    }

    Single<ProductStatus> queryProduct() {
        return getSubscriptionId()
                .flatMap(product -> product.isEmpty()
                                    ? Single.just(ProductStatus.fromNoProduct())
                                    : queryProduct(product.id).map(ProductStatus::fromSuccess))
                .observeOn(AndroidSchedulers.mainThread());
    }

    Single<String> purchase(final String id) {
        final ApiRequest request = ApiRequest.post(ApiEndpoints.CHECKOUT.path())
                                             .forPrivateApi()
                                             .withContent(new StartCheckout(id))
                                             .build();
        return apiClient.mappedResponse(request, CheckoutStarted.class)
                        .subscribeOn(scheduler)
                        .map(started -> started.token)
                        .doOnSuccess(token -> {
                            tokenStorage.setCheckoutToken(token);
                            playBilling.startPurchase(id, token);
                        })
                        .observeOn(AndroidSchedulers.mainThread());
    }

    Single<PurchaseStatus> verify(final Payload payload) {
        return update(payload)
                .flatMap(new Function<PurchaseStatus, Single<PurchaseStatus>>() {
                    @Override
                    public Single<PurchaseStatus> apply(@NonNull PurchaseStatus purchaseStatus) throws Exception {
                        if (purchaseStatus.isPending()) {
                            return pollStatus();
                        }
                        return Single.just(PurchaseStatus.UPDATE_FAIL);                    }
                })
                .doOnSuccess(ignore -> tokenStorage.clear())
                .doOnError(ignore -> tokenStorage.clear())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Single<PurchaseStatus> update(final Payload payload) {
        return apiClient.response(buildUpdateRequest(UpdateCheckout.fromSuccess(payload)))
                  .subscribeOn(scheduler)
                  .map(apiResponse -> apiResponse.isSuccess()
                                      ? PurchaseStatus.PENDING
                                      : PurchaseStatus.UPDATE_FAIL);
    }

    private Single<PurchaseStatus> pollStatus() {
        return Observable.interval(VERIFY_THROTTLE_SECONDS, TimeUnit.SECONDS, scheduler)
                         .take(4)
                         .flatMap(tick -> getStatus().toObservable())
                         .filter(status -> !status.isPending())
                         .first(PurchaseStatus.VERIFY_TIMEOUT);
    }

    private Single<PurchaseStatus> getStatus() {
        final ApiRequest request =
                ApiRequest.get(ApiEndpoints.CHECKOUT_URN.path(tokenStorage.getCheckoutToken()))
                          .forPrivateApi()
                          .build();
        return apiClient.mappedResponse(request, CheckoutUpdated.class)
                  .subscribeOn(scheduler)
                  .map(CheckoutUpdated.TO_STATUS);
    }

    public Single<ApiResponse> cancel(final String reason) {
        return apiClient.response(buildUpdateRequest(UpdateCheckout.fromFailure(reason)))
                        .doOnSuccess(ignore -> tokenStorage.clear())
                        .doOnError(ignore -> tokenStorage.clear())
                        .subscribeOn(scheduler);
    }

    private ApiRequest buildUpdateRequest(UpdateCheckout update) {
        return ApiRequest.post(ApiEndpoints.CHECKOUT_URN.path(tokenStorage.getCheckoutToken()))
                         .forPrivateApi()
                         .withContent(update)
                         .build();
    }

    private Single<ProductDetails> queryProduct(String id) {
        return playBilling.getDetails(id)
                          .subscribeOn(scheduler);
    }

    private Single<Product> getSubscriptionId() {
        return fetchAvailableProducts()
                .map(AvailableProducts.TO_PRODUCT);
    }

    private Single<AvailableProducts> fetchAvailableProducts() {
        final ApiRequest request =
                ApiRequest.get(ApiEndpoints.NATIVE_PRODUCTS.path())
                          .forPrivateApi()
                          .build();
        return apiClient.mappedResponse(request, AvailableProducts.class).subscribeOn(scheduler);
    }

}
