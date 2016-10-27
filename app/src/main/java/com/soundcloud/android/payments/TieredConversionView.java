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
import android.widget.Button;
import android.widget.TextView;

import javax.inject.Inject;

class TieredConversionView {

    private static final String RESTRICTIONS_DIALOG_TAG = "product_info";

    private final Resources resources;

    private FragmentManager fragmentManager;

    @Bind(R.id.conversion_buy) LoadingButton buyButton;
    @Bind(R.id.conversion_price) TextView priceView;
    @Bind(R.id.conversion_restrictions) TextView restrictionsView;
    @Bind(R.id.conversion_more_products) Button moreButton;
    @Bind(R.id.conversion_close) View closeButton;

    @Inject
    TieredConversionView(Resources resources) {
        this.resources = resources;
    }

    interface Listener {
        void onPurchasePrimary();
        void onMoreProducts();
        void onClose();
    }

    void setupContentView(AppCompatActivity activity, Listener listener) {
        this.fragmentManager = activity.getSupportFragmentManager();
        ButterKnife.bind(this, activity.findViewById(android.R.id.content));
        setupListener(listener);
    }

    private void setupListener(final Listener listener) {
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.conversion_buy:
                        listener.onPurchasePrimary();
                        break;
                    case R.id.conversion_close:
                        listener.onClose();
                        break;
                    case R.id.conversion_more_products:
                        listener.onMoreProducts();
                        break;
                    default:
                        throw new IllegalArgumentException("Click on unknown View ID");
                }
            }
        };
        buyButton.setOnClickListener(clickListener);
        closeButton.setOnClickListener(clickListener);
        moreButton.setOnClickListener(clickListener);
    }

    void showDetails(String price, int trialDays) {
        priceView.setText(resources.getString(R.string.conversion_price, price));
        priceView.setVisibility(View.VISIBLE);
        showTrialDays(trialDays);
        enableBuyButton();
    }

    void showPromo(String promoPrice, int promoDays, String regularPrice) {
        String duration = formatPromoDuration(promoDays);
        priceView.setText(resources.getString(R.string.conversion_price_promo, duration, promoPrice));
        priceView.setVisibility(View.VISIBLE);
        buyButton.setActionText(resources.getString(R.string.conversion_buy_promo));
        setupPromoRestrictions(duration, promoPrice, regularPrice);
        enableBuyButton();
    }

    private void showTrialDays(final int trialDays) {
        buyButton.setActionText(trialDays > 0
                ? resources.getString(R.string.conversion_buy_trial, trialDays)
                : resources.getString(R.string.conversion_buy_no_trial));
        setupRestrictions(trialDays);
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
        restrictionsView.setVisibility(View.VISIBLE);
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
        restrictionsView.setVisibility(View.VISIBLE);
    }

    private void enableBuyButton() {
        buyButton.setEnabled(true);
        buyButton.setLoading(false);
    }

    void showLoadingState() {
        buyButton.setEnabled(false);
        buyButton.setLoading(true);
    }

    void showRetryState() {
        buyButton.setEnabled(true);
        buyButton.setRetry();
    }

    void enableMorePlans() {
        moreButton.setVisibility(View.VISIBLE);
    }

}
