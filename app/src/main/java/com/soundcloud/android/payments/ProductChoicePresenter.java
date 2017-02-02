package com.soundcloud.android.payments;

import com.soundcloud.android.R;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import dagger.Lazy;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.View;

import javax.inject.Inject;

class ProductChoicePresenter extends DefaultActivityLightCycle<AppCompatActivity> implements ProductChoicePagerView.Listener {

    private static final String RESTRICTIONS_DIALOG_TAG = "restrictions_dialog";

    private final Lazy<ProductChoicePagerView> pagerView;
    private final Lazy<ProductChoiceScrollView> scrollView;
    private final ProductInfoFormatter formatter;

    private AppCompatActivity activity;

    @Inject
    ProductChoicePresenter(Lazy<ProductChoicePagerView> pagerView, Lazy<ProductChoiceScrollView> scrollView, ProductInfoFormatter formatter) {
        this.pagerView = pagerView;
        this.scrollView = scrollView;
        this.formatter = formatter;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        this.activity = activity;
        final View contentRoot = activity.findViewById(android.R.id.content);
        ProductChoiceView view = contentRoot.findViewById(R.id.product_choice_pager) == null
                ? scrollView.get()
                : pagerView.get();

        if (activity.getIntent().hasExtra(ProductChoiceActivity.AVAILABLE_PRODUCTS)) {
            AvailableWebProducts products = activity.getIntent().getParcelableExtra(ProductChoiceActivity.AVAILABLE_PRODUCTS);
            view.setupContent(contentRoot, products, this);
        } else {
            activity.finish();
        }
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        this.activity = null;
    }

    @Override
    public void onPurchaseProduct(WebProduct product) {
        startWebCheckout(product);
        activity.finish();
    }

    @Override
    public void onRestrictionsClick(WebProduct product) {
        if (product.hasPromo()) {
            showDialog(ConversionRestrictionsDialog.createForPromo(formatter.promoDuration(product.getPromoDays()), product.getPromoPrice().get(), product.getPrice()));
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

}
