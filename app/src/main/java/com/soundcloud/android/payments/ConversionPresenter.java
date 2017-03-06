package com.soundcloud.android.payments;

import static com.soundcloud.android.payments.error.PlanConversionErrorDialog.createWithMessage;
import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import com.soundcloud.android.R;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialogFragment;

import javax.inject.Inject;

class ConversionPresenter extends DefaultActivityLightCycle<AppCompatActivity> implements ConversionView.Listener {

    private static final String PLAN_CONVERSION_ERROR_DIALOG_TAG = "plan_conversion_error_dialog";

    @VisibleForTesting
    static final String LOADED_PRODUCTS = "available_products";

    private final WebPaymentOperations operations;
    private final ConversionView view;
    private final EventBus eventBus;
    private final FeatureOperations featureOperations;

    private Subscription subscription = RxUtils.invalidSubscription();
    private AvailableWebProducts products = AvailableWebProducts.empty();
    private AppCompatActivity activity;

    @Inject
    ConversionPresenter(WebPaymentOperations operations,
                        ConversionView view,
                        EventBus eventBus,
                        FeatureOperations featureOperations) {
        this.operations = operations;
        this.view = view;
        this.eventBus = eventBus;
        this.featureOperations = featureOperations;
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
            if (shouldShowPlanChoice()) {
                view.enableMorePlans();
            }
        } else {
            view.showRetryState();
        }
    }

    private boolean shouldShowPlanChoice() {
        return products.midTier().isPresent()
                && !featureOperations.getCurrentPlan().isGoPlan();
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
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forConversionPromoImpression());
    }

    private void displayDefault(WebProduct product) {
        view.showDetails(product.getDiscountPrice().or(product.getPrice()), product.getTrialDays());
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forConversionBuyButtonImpression());
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
            attemptWebCheckout(products.highTier().get());
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

    private void attemptWebCheckout(WebProduct product) {
        if (featureOperations.isPlanManageable()) {
            startWebCheckout(product);
        } else {
            showPlanConversionErrorDialog();
        }
    }

    private void startWebCheckout(WebProduct product) {
        eventBus.publish(EventQueue.TRACKING, product.hasPromo()
                ? UpgradeFunnelEvent.forConversionPromoClick()
                : UpgradeFunnelEvent.forConversionBuyButtonClick());

        final Intent intent = new Intent(activity, WebCheckoutActivity.class);
        intent.putExtra(WebCheckoutPresenter.PRODUCT_INFO, product);
        activity.startActivity(intent);
        activity.finish();
    }

    private void showPlanConversionErrorDialog() {
        if (featureOperations.isPlanVendorApple()) {
            showDialog(createWithMessage(activity.getString(R.string.plan_conversion_error_message_apple)));
        } else {
            showDialog(createWithMessage(activity.getString(R.string.plan_conversion_error_message_generic)));
        }
    }

    private void showDialog(AppCompatDialogFragment dialogFragment) {
        dialogFragment.show(activity.getSupportFragmentManager(), PLAN_CONVERSION_ERROR_DIALOG_TAG);
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
