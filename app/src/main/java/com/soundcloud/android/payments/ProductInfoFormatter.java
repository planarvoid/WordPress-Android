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

}
