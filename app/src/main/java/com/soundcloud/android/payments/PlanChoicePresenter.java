package com.soundcloud.android.payments;

import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

class PlanChoicePresenter extends DefaultActivityLightCycle<AppCompatActivity> implements PlanChoiceView.Listener {

    private final PlanChoiceView view;

    private Activity activity;
    private AvailableWebProducts products;

    @Inject
    PlanChoicePresenter(PlanChoiceView view) {
        this.view = view;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        this.activity = activity;
        view.setupContentView(activity, this);
        if (activity.getIntent().hasExtra(PlanChoiceActivity.AVAILABLE_PRODUCTS)) {
            products = activity.getIntent().getParcelableExtra(PlanChoiceActivity.AVAILABLE_PRODUCTS);
            displayProducts();
        } else {
            activity.finish();
        }
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        this.activity = null;
    }

    private void displayProducts() {
        view.displayChoices(products);
    }

    @Override
    public void onPurchaseMidTier() {
        startWebCheckout(products.midTier().get());
        activity.finish();
    }

    @Override
    public void onPurchaseHighTier() {
        startWebCheckout(products.highTier().get());
        activity.finish();
    }

    private void startWebCheckout(WebProduct product) {
        final Intent intent = new Intent(activity, WebCheckoutActivity.class);
        intent.putExtra(WebCheckoutPresenter.PRODUCT_INFO, product);
        activity.startActivity(intent);
        activity.finish();
    }

}
