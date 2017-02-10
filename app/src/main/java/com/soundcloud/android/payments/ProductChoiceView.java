package com.soundcloud.android.payments;

import android.view.View;

abstract class ProductChoiceView {

    abstract void setupContent(View view, AvailableWebProducts products, Listener listener);

    interface Listener {
        void onBuyImpression(WebProduct product);
        void onBuyClick(WebProduct product);
        void onRestrictionsClick(WebProduct product);
    }

}
