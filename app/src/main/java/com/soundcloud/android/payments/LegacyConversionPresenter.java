package com.soundcloud.android.payments;

import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

class LegacyConversionPresenter extends DefaultActivityLightCycle<AppCompatActivity> implements ConversionView.Listener {

    @VisibleForTesting
    static final String LOADED_PRODUCT = "product_info";

    private final WebPaymentOperations operations;
    private final ConversionView conversionView;
    private final EventBus eventBus;

    private Subscription subscription = RxUtils.invalidSubscription();
    private AppCompatActivity activity;
    private Optional<WebProduct> product = Optional.absent();

    @Inject
    LegacyConversionPresenter(WebPaymentOperations operations, ConversionView conversionView, EventBus eventBus) {
        this.operations = operations;
        this.conversionView = conversionView;
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        this.activity = activity;
        conversionView.setupContentView(activity, this);
        if (bundle != null && bundle.getParcelable(LOADED_PRODUCT) != null) {
            product = Optional.fromNullable((WebProduct) bundle.getParcelable(LOADED_PRODUCT));
            enablePurchase(product.get());
        } else {
            loadProducts();
        }
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        subscription.unsubscribe();
        this.activity = null;
    }

    private void loadProducts() {
        conversionView.setBuyButtonLoading();
        subscription = operations.products()
                                 .observeOn(AndroidSchedulers.mainThread())
                                 .subscribe(new WebProductsSubscriber());
    }

    @Override
    public void startPurchase() {
        if (product.isPresent()) {
            startWebCheckout();
        } else {
            loadProducts();
        }
    }

    @Override
    public void close() {
        activity.supportFinishAfterTransition();
    }

    private void startWebCheckout() {
        WebProduct loadedProduct = product.get();

        eventBus.publish(EventQueue.TRACKING, loadedProduct.hasPromo()
                ? UpgradeFunnelEvent.forUpgradePromoClick()
                : UpgradeFunnelEvent.forUpgradeButtonClick());

        final Intent intent = new Intent(activity, WebCheckoutActivity.class);
        intent.putExtra(WebCheckoutPresenter.PRODUCT_INFO, loadedProduct);
        activity.startActivity(intent);
        activity.finish();
    }

    @Override
    public void onSaveInstanceState(AppCompatActivity activity, Bundle bundle) {
        if (product.isPresent()) {
            bundle.putParcelable(LOADED_PRODUCT, product.get());
        }
    }

    private void enablePurchase(WebProduct product) {
        if (product.hasPromo()) {
            displayPromo(product);
        } else {
            displayPurchase(product);
        }
    }

    private void displayPromo(WebProduct product) {
        checkNotNull(product.getPromoPrice());
        conversionView.showPromo(product.getPromoPrice().get(), product.getPromoDays(), product.getPrice());
        conversionView.setBuyButtonReady();
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forUpgradePromoImpression());
    }

    private void displayPurchase(WebProduct product) {
        conversionView.showPrice(product.getDiscountPrice().or(product.getPrice()), product.getTrialDays());
        conversionView.setBuyButtonReady();
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forUpgradeButtonImpression());
    }

    private class WebProductsSubscriber extends DefaultSubscriber<AvailableWebProducts> {
        @Override
        public void onNext(AvailableWebProducts products) {
            if (products.highTier().isPresent()) {
                enablePurchase(products.highTier().get());
            }
            product = products.highTier();
        }

        @Override
        public void onError(Throwable e) {
            conversionView.setBuyButtonRetry();
        }
    }

}
