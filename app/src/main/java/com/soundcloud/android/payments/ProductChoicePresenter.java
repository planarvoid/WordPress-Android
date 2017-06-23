package com.soundcloud.android.payments;

import com.soundcloud.android.R;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSingleObserver;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBusV2;
import dagger.Lazy;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.View;
import android.widget.Toast;

import javax.inject.Inject;

class ProductChoicePresenter extends DefaultActivityLightCycle<AppCompatActivity> implements ProductChoicePagerView.Listener {

    private static final String RESTRICTIONS_DIALOG_TAG = "restrictions_dialog";

    private final WebPaymentOperations operations;
    private final Lazy<ProductChoicePagerView> pagerView;
    private final Lazy<ProductChoiceScrollView> scrollView;
    private final ProductInfoFormatter formatter;
    private final EventBusV2 eventBus;

    private Disposable disposable = RxUtils.emptyDisposable();
    private AppCompatActivity activity;

    @Inject
    ProductChoicePresenter(WebPaymentOperations operations,
                           Lazy<ProductChoicePagerView> pagerView,
                           Lazy<ProductChoiceScrollView> scrollView,
                           ProductInfoFormatter formatter,
                           EventBusV2 eventBus) {
        this.operations = operations;
        this.pagerView = pagerView;
        this.scrollView = scrollView;
        this.formatter = formatter;
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        this.activity = activity;

        if (activity.getIntent().hasExtra(ProductChoiceActivity.AVAILABLE_PRODUCTS)) {
            AvailableWebProducts products = activity.getIntent().getParcelableExtra(ProductChoiceActivity.AVAILABLE_PRODUCTS);
            displayProducts(products);
        } else {
            loadProducts();
        }
    }

    private void displayProducts(AvailableWebProducts products) {
        final View contentRoot = activity.findViewById(android.R.id.content);
        ProductChoiceView productChoiceView = contentRoot.findViewById(R.id.product_choice_pager) == null
                                 ? scrollView.get()
                                 : pagerView.get();
        productChoiceView.showContent(contentRoot, products, this, getInitialPlan());
    }

    private Plan getInitialPlan() {
        if (activity.getIntent().hasExtra(ProductChoiceActivity.DEFAULT_PLAN)) {
            return (Plan) activity.getIntent().getSerializableExtra(ProductChoiceActivity.DEFAULT_PLAN);
        }
        return Plan.MID_TIER;
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        disposable.dispose();
        this.activity = null;
    }

    @Override
    public void onBuyImpression(WebProduct product) {
        switch (Plan.fromId(product.getPlanId())) {
            case MID_TIER:
                eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forChooserBuyMidTierImpression());
                break;
            case HIGH_TIER:
                eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forChooserBuyHighTierImpression());
                break;
            default:
                logTrackingError();
        }
    }

    private void loadProducts() {
        disposable = operations.products()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new WebProductsObserver());
    }

    @Override
    public void onBuyClick(WebProduct product) {
        startWebCheckout(product);
        trackBuyButtonClick(product);
        activity.finish();
    }

    private void trackBuyButtonClick(WebProduct product) {
        switch (Plan.fromId(product.getPlanId())) {
            case MID_TIER:
                eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forChooserBuyMidTierClick());
                break;
            case HIGH_TIER:
                eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forChooserBuyHighTierClick());
                break;
            default:
                logTrackingError();
        }
    }

    private void logTrackingError() {
        ErrorUtils.handleSilentException(new IllegalStateException("Dropping funnel tracking event: failed to resolve tier from product"));
    }

    @Override
    public void onRestrictionsClick(WebProduct product) {
        if (product.hasPromo()) {
            showDialog(ConversionRestrictionsDialog.createForPromo(formatter.promoDuration(product.getPromoDays()), product.getPromoPriceData().get().format(), product.getPriceData().format()));
        } else if (product.getTrialDays() > 0) {
            showDialog(ConversionRestrictionsDialog.createForTrial(product.getTrialDays()));
        } else {
            showDialog(ConversionRestrictionsDialog.createForNoTrial());
        }
    }

    private void showDialog(AppCompatDialogFragment dialogFragment) {
        dialogFragment.show(activity.getSupportFragmentManager(), RESTRICTIONS_DIALOG_TAG);
    }

    private void startWebCheckout(WebProduct product) {
        final Intent intent = new Intent(activity, WebCheckoutActivity.class);
        intent.putExtra(WebCheckoutPresenter.PRODUCT_INFO, product);
        activity.startActivity(intent);
        activity.finish();
    }

    private class WebProductsObserver extends DefaultSingleObserver<AvailableWebProducts> {

        @Override
        public void onSuccess(@NonNull AvailableWebProducts products) {
            if (products.highTier().isPresent() && products.midTier().isPresent()) {
                saveProductsInIntent(products);
                displayProducts(products);
            } else {
                showErrorAndFinishActivity();
            }
            super.onSuccess(products);
        }

        @Override
        public void onError(Throwable e) {
            showErrorAndFinishActivity();
        }
    }

    private void saveProductsInIntent(AvailableWebProducts products) {
        activity.getIntent().putExtra(ProductChoiceActivity.AVAILABLE_PRODUCTS, products);
    }

    private void showErrorAndFinishActivity() {
        Toast.makeText(activity,
                       R.string.product_choice_error_unavailable,
                       Toast.LENGTH_SHORT).show();
        activity.finish();
    }
}
