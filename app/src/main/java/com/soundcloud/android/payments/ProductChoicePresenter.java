package com.soundcloud.android.payments;

import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

class ProductChoicePresenter extends DefaultActivityLightCycle<AppCompatActivity> implements ProductChoiceView.Listener {

    private final ProductChoiceView view;

    private Activity activity;

    @Inject
    ProductChoicePresenter(ProductChoiceView view) {
        this.view = view;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        this.activity = activity;
        view.setupContentView(activity, this);
        if (activity.getIntent().hasExtra(ProductChoiceActivity.AVAILABLE_PRODUCTS)) {
            AvailableWebProducts products = activity.getIntent().getParcelableExtra(ProductChoiceActivity.AVAILABLE_PRODUCTS);
            view.displayOptions(products);
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
