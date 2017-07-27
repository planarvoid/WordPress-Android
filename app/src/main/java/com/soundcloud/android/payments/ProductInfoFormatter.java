package com.soundcloud.android.payments;

import com.soundcloud.android.R;

import android.content.res.Resources;

import javax.inject.Inject;

class ProductInfoFormatter {

    private final Resources resources;

    @Inject
    ProductInfoFormatter(Resources resources) {
        this.resources = resources;
    }

    String monthlyPricing(String price) {
        return resources.getString(R.string.conversion_price, price);
    }

    String promoPricing(int days, String price) {
        return resources.getString(R.string.conversion_price_promo, promoDuration(days), price);
    }

    String promoDuration(int days) {
        if (days >= 30) {
            int months = days / 30;
            return resources.getQuantityString(R.plurals.elapsed_months, months, months);
        } else {
            return resources.getQuantityString(R.plurals.elapsed_days, days, days);
        }
    }

    String buyButton(int trialDays) {
        return trialDays > 0
               ? resources.getString(R.string.conversion_buy_trial, trialDays)
               : resources.getString(R.string.conversion_buy_no_trial);
    }

    String configuredPrice(WebProduct product) {
        if (product.hasPromo()) {
            return promoPricing(product.getPromoDays(), product.getPromoPriceData().get().format());
        } else if (product.getDiscountPriceData().isPresent()) {
            return monthlyPricing(product.getDiscountPriceData().get().format());
        } else {
            return monthlyPricing(product.getPriceData().format());
        }
    }

    String configuredBuyButton(WebProduct product) {
        return product.hasPromo()
                ? resources.getString(R.string.conversion_buy_promo)
                : buyButton(product.getTrialDays());
    }

    String configuredRestrictionsText(WebProduct product) {
        if (product.hasPromo()) {
            return resources.getString(R.string.conversion_restrictions_promo, product.getPriceData().format());
        } else {
            return resources.getString(R.string.conversion_restrictions);
        }
    }

}
