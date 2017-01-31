package com.soundcloud.android.payments;

import com.soundcloud.android.R;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import dagger.Lazy;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import javax.inject.Inject;

class ProductChoicePresenter extends DefaultActivityLightCycle<AppCompatActivity> implements ProductChoicePagerView.Listener {

    private final Lazy<ProductChoicePagerView> pagerView;
    private final Lazy<ProductChoiceScrollView> scrollView;

    private Activity activity;

    @Inject
    ProductChoicePresenter(Lazy<ProductChoicePagerView> pagerView, Lazy<ProductChoiceScrollView> scrollView) {
        this.pagerView = pagerView;
        this.scrollView = scrollView;
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

    private void startWebCheckout(WebProduct product) {
        final Intent intent = new Intent(activity, WebCheckoutActivity.class);
        intent.putExtra(WebCheckoutPresenter.PRODUCT_INFO, product);
        activity.startActivity(intent);
        activity.finish();
    }

}
