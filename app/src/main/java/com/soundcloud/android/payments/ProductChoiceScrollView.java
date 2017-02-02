package com.soundcloud.android.payments;

import butterknife.ButterKnife;
import com.soundcloud.android.R;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import javax.inject.Inject;

class ProductChoiceScrollView extends ProductChoiceView {

    private final ProductInfoFormatter formatter;

    @Inject
    ProductChoiceScrollView(ProductInfoFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    void setupContent(View view, AvailableWebProducts products, Listener listener) {
        configureView(ButterKnife.findById(view, R.id.buy_mid_tier), products.midTier().get(), listener);
        configureView(ButterKnife.findById(view, R.id.buy_high_tier), products.highTier().get(), listener);
    }

    private void configureView(View view, WebProduct product, Listener listener) {
        TextView price = ButterKnife.findById(view, R.id.product_choice_price);
        price.setText(formatter.configuredPrice(product));
        configureBuyButton(product, ButterKnife.findById(view, R.id.buy), listener);
        configureRestrictions(product, ButterKnife.findById(view, R.id.product_choice_restrictions), listener);
    }

    private void configureBuyButton(WebProduct product, Button buyButton, Listener listener) {
        buyButton.setText(formatter.configuredBuyButton(product));
        buyButton.setOnClickListener(v -> listener.onPurchaseProduct(product));
    }

    private void configureRestrictions(WebProduct product, TextView restrictions, Listener listener) {
        restrictions.setText(formatter.configuredRestrictionsText(product));
        restrictions.setOnClickListener(v -> listener.onRestrictionsClick(product));
    }

}
