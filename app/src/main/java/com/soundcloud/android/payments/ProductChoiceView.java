package com.soundcloud.android.payments;

import com.soundcloud.android.configuration.Plan;

import android.view.View;

abstract class ProductChoiceView {

    abstract void showContent(View view, AvailableWebProducts products, Listener listener, Plan initialPlan);

    interface Listener {
        void onBuyImpression(WebProduct product);
        void onBuyClick(WebProduct product);
        void onRestrictionsClick(WebProduct product);
    }

}
