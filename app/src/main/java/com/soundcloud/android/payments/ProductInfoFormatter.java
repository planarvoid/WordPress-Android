package com.soundcloud.android.payments;

import static com.soundcloud.java.checks.Preconditions.checkNotNull;

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
            checkNotNull(product.getPromoPrice());
            return promoPricing(product.getPromoDays(), product.getPromoPrice().get());
        } else {
            return monthlyPricing(product.getDiscountPrice().or(product.getPrice()));
        }
    }

    String configuredBuyButton(WebProduct product) {
        return product.hasPromo()
                ? resources.getString(R.string.conversion_buy_promo)
                : buyButton(product.getTrialDays());
    }

    String configuredRestrictionsText(WebProduct product) {
        if (product.hasPromo()) {
            return resources.getString(R.string.conversion_restrictions_promo, product.getPrice());
        } else {
            return resources.getString(R.string.conversion_restrictions);
        }
    }

}
