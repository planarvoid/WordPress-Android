package com.soundcloud.android.payments;

import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

class ConversionPresenter extends DefaultActivityLightCycle<AppCompatActivity> implements ConversionView.Listener {

    @VisibleForTesting
    static final String LOADED_PRODUCTS = "available_products";

    private final WebPaymentOperations operations;
    private final ConversionView view;
    private final EventBus eventBus;
    private final FeatureFlags featureFlags;

    private Subscription subscription = RxUtils.invalidSubscription();
    private AvailableWebProducts products = AvailableWebProducts.empty();
    private Activity activity;

    @Inject
    ConversionPresenter(WebPaymentOperations operations,
                        ConversionView view,
                        EventBus eventBus,
                        FeatureFlags featureFlags) {
        this.operations = operations;
        this.view = view;
        this.eventBus = eventBus;
        this.featureFlags = featureFlags;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        this.activity = activity;
        view.setupContentView(activity, this);
        if (bundle != null && bundle.getParcelable(LOADED_PRODUCTS) != null) {
            products = bundle.getParcelable(LOADED_PRODUCTS);
            displayProducts();
        } else {
            loadProducts();
        }
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        subscription.unsubscribe();
        this.activity = null;
    }

    @Override
    public void onSaveInstanceState(AppCompatActivity activity, Bundle bundle) {
        bundle.putParcelable(LOADED_PRODUCTS, products);
    }

    private void displayProducts() {
        if (products.highTier().isPresent()) {
            displayPrimaryProduct(products.highTier().get());
            if (featureFlags.isEnabled(Flag.MID_TIER) && products.midTier().isPresent()) {
                view.enableMorePlans();
            }
        } else {
            view.showRetryState();
        }
    }

    private void displayPrimaryProduct(WebProduct product) {
        if (product.hasPromo()) {
            displayPromo(product);
        } else {
            displayDefault(product);
        }
    }

    private void displayPromo(WebProduct product) {
        checkNotNull(product.getPromoPrice());
        view.showPromo(product.getPromoPrice().get(), product.getPromoDays(), product.getPrice());
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forUpgradePromoImpression());
    }

    private void displayDefault(WebProduct product) {
        view.showDetails(product.getDiscountPrice().or(product.getPrice()), product.getTrialDays());
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forUpgradeButtonImpression());
    }

    private void loadProducts() {
        view.showLoadingState();
        subscription = operations.products()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new WebProductsSubscriber());
    }

    @Override
    public void onPurchasePrimary() {
        if (products.highTier().isPresent()) {
            startWebCheckout(products.highTier().get());
        } else {
            loadProducts();
        }
    }

    @Override
    public void onMoreProducts() {
        final Intent intent = new Intent(activity, ProductChoiceActivity.class);
        intent.putExtra(ProductChoiceActivity.AVAILABLE_PRODUCTS, products);
        activity.startActivity(intent);
        activity.finish();
    }

    private void startWebCheckout(WebProduct product) {
        eventBus.publish(EventQueue.TRACKING, product.hasPromo()
                ? UpgradeFunnelEvent.forUpgradePromoClick()
                : UpgradeFunnelEvent.forUpgradeButtonClick());

        final Intent intent = new Intent(activity, WebCheckoutActivity.class);
        intent.putExtra(WebCheckoutPresenter.PRODUCT_INFO, product);
        activity.startActivity(intent);
        activity.finish();
    }

    private class WebProductsSubscriber extends DefaultSubscriber<AvailableWebProducts> {
        @Override
        public void onNext(AvailableWebProducts result) {
            products = result;
            displayProducts();
        }

        @Override
        public void onError(Throwable e) {
            view.showRetryState();
        }
    }

}
