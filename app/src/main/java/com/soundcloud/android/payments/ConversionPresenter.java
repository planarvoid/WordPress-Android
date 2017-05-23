package com.soundcloud.android.payments;

import static com.soundcloud.android.payments.error.PlanConversionErrorDialog.createWithMessage;
import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import com.soundcloud.android.R;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSingleObserver;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

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

    private Disposable disposable = RxUtils.emptyDisposable();
    private AvailableWebProducts products = AvailableWebProducts.empty();
    private AppCompatActivity activity;

    private UpsellContext upsellContext = UpsellContext.DEFAULT;
    private Optional<WebProduct> primaryProduct = Optional.absent();

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
        upsellContext = UpsellContext.from(activity.getIntent());
        view.setupContentView(activity, this);
        configureCopy();
        if (bundle != null && bundle.getParcelable(LOADED_PRODUCTS) != null) {
            products = bundle.getParcelable(LOADED_PRODUCTS);
            displayProducts();
        } else {
            loadProducts();
        }
    }

    private void configureCopy() {
        if (isMidTierUser()) {
            view.setText(R.string.tier_plus, R.string.conversion_title_upgrade, R.string.conversion_description_upgrade);
        } else {
            setCopyFromUpsellContext();
        }
    }

    private void setCopyFromUpsellContext() {
        switch (upsellContext) {
            case ADS:
                view.setText(R.string.tier_go, R.string.conversion_title_ads_focus, R.string.conversion_description_mt);
                break;
            case OFFLINE:
                view.setText(R.string.tier_go, R.string.conversion_title_offline_focus, R.string.conversion_description_mt);
                break;
            case PREMIUM_CONTENT:
                view.setText(R.string.tier_plus, R.string.conversion_title_ht, R.string.conversion_description_ht);
                break;
            default:
                view.setText(R.string.tier_go, R.string.conversion_title_mt, R.string.conversion_description_mt);
        }
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        disposable.dispose();
        this.activity = null;
    }

    @Override
    public void onSaveInstanceState(AppCompatActivity activity, Bundle bundle) {
        bundle.putParcelable(LOADED_PRODUCTS, products);
    }

    private void displayProducts() {
        if (canPurchaseMidTier() && upsellContext != UpsellContext.PREMIUM_CONTENT) {
            enableMidTierPurchase();
        } else if (products.highTier().isPresent()) {
            enableHighTierPurchase();
        } else {
            view.showRetryState();
        }
    }

    private void enableMidTierPurchase() {
        displayPrimaryProduct(products.midTier().get());
        view.enableMoreForHighTier();
    }

    private void enableHighTierPurchase() {
        displayPrimaryProduct(products.highTier().get());
        if (canPurchaseMidTier()) {
            view.enableMoreForMidTier(products.midTier().get().getPrice());
        }
    }

    private boolean canPurchaseMidTier() {
        return products.midTier().isPresent()
                && products.highTier().isPresent()
                && !isMidTierUser();
    }

    private boolean isMidTierUser() {
        return featureOperations.getCurrentPlan().isGoPlan();
    }

    private void displayPrimaryProduct(WebProduct product) {
        primaryProduct = Optional.of(product);
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
        disposable = operations.products()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new WebProductsObserver());
    }

    @Override
    public void onPurchasePrimary() {
        if (primaryProduct.isPresent()) {
            attemptWebCheckout(primaryProduct.get());
        } else {
            loadProducts();
        }
    }

    @Override
    public void onMoreProducts() {
        final Intent intent = new Intent(activity, ProductChoiceActivity.class);
        intent.putExtra(ProductChoiceActivity.AVAILABLE_PRODUCTS, products);
        if (upsellContext != UpsellContext.PREMIUM_CONTENT) {
            intent.putExtra(ProductChoiceActivity.DEFAULT_PLAN, Plan.HIGH_TIER);
        }
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

    private class WebProductsObserver extends DefaultSingleObserver<AvailableWebProducts> {

        @Override
        public void onSuccess(AvailableWebProducts result) {
            products = result;
            displayProducts();
            super.onSuccess(result);
        }

        @Override
        public void onError(Throwable e) {
            view.showRetryState();
        }
    }

}
