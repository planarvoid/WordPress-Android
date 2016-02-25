package com.soundcloud.android.payments;

import butterknife.Bind;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.view.LoadingButton;

import android.content.res.Resources;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import javax.inject.Inject;

class ConversionView {

    private final Resources resources;

    @Bind(R.id.conversion_buy) LoadingButton buyButton;
    @Bind(R.id.conversion_price) TextView priceView;
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
    }

    public void showPrice(String price) {
        priceView.setText(resources.getString(R.string.conversion_price, price));
        priceView.setVisibility(View.VISIBLE);
    }

    public void showTrialDays(int trialDays) {
        buyButton.setActionText(trialDays > 0
                ? resources.getString(R.string.conversion_buy_trial, trialDays)
                : resources.getString(R.string.conversion_buy_no_trial));
    }

    public void setBuyButtonReady() {
        buyButton.setEnabled(true);
        buyButton.setLoading(false);
    }

    public void setBuyButtonLoading() {
        buyButton.setEnabled(false);
        buyButton.setLoading(true);
    }

    public void setBuyButtonRetry() {
        buyButton.setEnabled(true);
        buyButton.setRetry();
    }

}
