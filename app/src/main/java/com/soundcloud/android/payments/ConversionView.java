package com.soundcloud.android.payments;

import butterknife.Bind;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.view.LoadingButton;

import android.content.res.Resources;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import javax.inject.Inject;

class ConversionView {

    private static final String RESTRICTIONS_DIALOG_TAG = "product_info";

    private final Resources resources;

    private FragmentManager fragmentManager;

    @Bind(R.id.conversion_buy) LoadingButton buyButton;
    @Bind(R.id.conversion_price) TextView priceView;
    @Bind(R.id.conversion_restrictions) TextView restrictionsView;
    @Bind(R.id.conversion_close) View closeButton;
    @Bind(R.id.conversion_outside) View outside;

    interface Listener {
        void startPurchase();
        void close();
    }

    @Inject
    public ConversionView(Resources resources) {
        this.resources = resources;
    }

    void setupContentView(AppCompatActivity activity, Listener listener) {
        this.fragmentManager = activity.getSupportFragmentManager();
        ButterKnife.bind(this, activity.findViewById(android.R.id.content));
        setListener(listener);
    }

    private void setListener(final Listener listener) {
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.conversion_buy:
                        listener.startPurchase();
                        break;
                    case R.id.conversion_close:
                    case R.id.conversion_outside:
                        listener.close();
                        break;
                    default:
                        throw new IllegalArgumentException("Click on unknown View ID");
                }
            }
        };
        buyButton.setOnClickListener(clickListener);
        closeButton.setOnClickListener(clickListener);
        outside.setOnClickListener(clickListener);
        restrictionsView.setOnClickListener(clickListener);
    }

    void showPrice(String price) {
        priceView.setText(resources.getString(R.string.conversion_price, price));
        priceView.setVisibility(View.VISIBLE);
    }

    void showPrice(String price, int trialDays) {
        showPrice(price);
        showTrialDays(trialDays);
    }

    private void showTrialDays(final int trialDays) {
        buyButton.setActionText(trialDays > 0
                ? resources.getString(R.string.conversion_buy_trial, trialDays)
                : resources.getString(R.string.conversion_buy_no_trial));
        setupRestrictions(trialDays);
    }

    void showPromo(String promoPrice, int promoDays, String regularPrice) {
        String duration = formatPromoDuration(promoDays);
        priceView.setText(resources.getString(R.string.conversion_price_promo, duration, promoPrice));
        priceView.setVisibility(View.VISIBLE);
        buyButton.setActionText(resources.getString(R.string.conversion_buy_promo));
        setupPromoRestrictions(duration, promoPrice, regularPrice);
    }

    @VisibleForTesting
    String formatPromoDuration(int promoDays) {
        if (promoDays >= 30) {
            int months = promoDays / 30;
            return resources.getQuantityString(R.plurals.elapsed_months, months, months);
        } else {
            return resources.getQuantityString(R.plurals.elapsed_days, promoDays, promoDays);
        }
    }

    private void setupRestrictions(final int trialDays) {
        restrictionsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ConversionRestrictionsDialog dialog = trialDays > 0
                        ? ConversionRestrictionsDialog.createForTrial(trialDays)
                        : ConversionRestrictionsDialog.createForNoTrial();
                dialog.show(fragmentManager, RESTRICTIONS_DIALOG_TAG);
            }
        });
    }

    private void setupPromoRestrictions(final String duration, final String promoPrice, final String regularPrice) {
        restrictionsView.setText(resources.getString(R.string.conversion_restrictions_promo, regularPrice));
        restrictionsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ConversionRestrictionsDialog.createForPromo(duration, promoPrice, regularPrice)
                        .show(fragmentManager, RESTRICTIONS_DIALOG_TAG);
            }
        });
    }

    void setBuyButtonReady() {
        buyButton.setEnabled(true);
        buyButton.setLoading(false);
    }

    void setBuyButtonLoading() {
        buyButton.setEnabled(false);
        buyButton.setLoading(true);
    }

    void setBuyButtonRetry() {
        buyButton.setEnabled(true);
        buyButton.setRetry();
    }

}
