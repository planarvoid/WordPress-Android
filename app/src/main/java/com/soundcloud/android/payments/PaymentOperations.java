package com.soundcloud.android.payments;

import com.soundcloud.android.payments.googleplay.PlayBillingService;
import com.soundcloud.android.rx.ScSchedulers;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

import android.app.Activity;

import javax.inject.Inject;

class PaymentOperations {

    private final PlayBillingService playBilling;

    private final Func1<String, Observable<ProductDetails>> DETAILS_BY_ID = new Func1<String, Observable<ProductDetails>>() {
        @Override
        public Observable<ProductDetails> call(String id) {
            return queryProductDetails(id);
        }
    };

    @Inject
    PaymentOperations(PlayBillingService playBilling) {
        this.playBilling = playBilling;
    }

    public Observable<ConnectionStatus> connect(Activity activity) {
        return playBilling.openConnection(activity);
    }

    public void disconnect() {
        playBilling.closeConnection();
    }

    public Observable<ProductDetails> queryProductDetails() {
        return querySubscriptionId()
                .flatMap(DETAILS_BY_ID)
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Observable<ProductDetails> queryProductDetails(String id) {
        return playBilling.getDetails(id)
                .subscribeOn(ScSchedulers.API_SCHEDULER);
    }

    private Observable<String> querySubscriptionId() {
        // TODO: The SKU will eventually come from /products/google-play on api-mobile
        return Observable.just("consumer_subscription");
    }

}
