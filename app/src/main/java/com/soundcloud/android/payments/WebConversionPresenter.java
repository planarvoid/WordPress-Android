package com.soundcloud.android.payments;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeTrackingEvent;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

class WebConversionPresenter extends DefaultActivityLightCycle<AppCompatActivity> implements ConversionView.Listener {

    static final String PRODUCT_INFO = "product_info";

    private final WebPaymentOperations operations;
    private final ConversionView conversionView;
    private final EventBus eventBus;

    private Subscription subscription = RxUtils.invalidSubscription();
    private AppCompatActivity activity;
    private Optional<WebProduct> product = Optional.absent();

    @Inject
    public WebConversionPresenter(WebPaymentOperations operations, ConversionView conversionView, EventBus eventBus) {
        this.operations = operations;
        this.conversionView = conversionView;
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        this.activity = activity;
        conversionView.setupContentView(activity, this);
        if (bundle != null && bundle.getParcelable(PRODUCT_INFO) != null) {
            product = Optional.fromNullable((WebProduct) bundle.getParcelable(PRODUCT_INFO));
            enablePurchase(product.get());
        } else {
            loadProduct();
        }
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        subscription.unsubscribe();
        this.activity = null;
    }

    private void loadProduct() {
        conversionView.setBuyButtonLoading();
        subscription = operations.product()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new WebProductSubscriber());
    }

    @Override
    public void startPurchase() {
        if (product.isPresent()) {
            startWebCheckout();
        } else {
            loadProduct();
        }
    }

    @Override
    public void close() {
        activity.supportFinishAfterTransition();
    }

    private void startWebCheckout() {
        eventBus.publish(EventQueue.TRACKING, UpgradeTrackingEvent.forUpgradeButtonClick());

        final Intent intent = new Intent(activity, WebCheckoutActivity.class);
        intent.putExtra(PRODUCT_INFO, product.get());
        activity.startActivity(intent);
        activity.finish();
    }

    @Override
    public void onSaveInstanceState(AppCompatActivity activity, Bundle bundle) {
        if (product.isPresent()) {
            bundle.putParcelable(PRODUCT_INFO, product.get());
        }
    }

    private void enablePurchase(WebProduct product) {
        conversionView.showPrice(product.getDiscountPrice().or(product.getPrice()));
        conversionView.showTrialDays(product.getTrialDays());
        conversionView.setBuyButtonReady();
        eventBus.publish(EventQueue.TRACKING, UpgradeTrackingEvent.forUpgradeButtonImpression());
    }

    class WebProductSubscriber extends DefaultSubscriber<Optional<WebProduct>> {
        @Override
        public void onNext(Optional<WebProduct> result) {
            if (result.isPresent()) {
                enablePurchase(result.get());
            }
            product = result;
        }

        @Override
        public void onError(Throwable e) {
            conversionView.setBuyButtonRetry();
        }
    }

}
