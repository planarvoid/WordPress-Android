package com.soundcloud.android.payments;

import butterknife.Bind;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.util.AnimUtils;

import android.content.res.Resources;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import javax.inject.Inject;

class UpgradeView  {

    private final Resources resources;

    @Bind(R.id.upgrade_header) View upgradeHeader;
    @Bind(R.id.success_header) View successHeader;
    @Bind(R.id.upgrade_buy) Button buyButton;
    @Bind(R.id.upgrade_loading) View loading;

    interface Listener {
        void startPurchase();
    }

    @Inject
    public UpgradeView(Resources resources) {
        this.resources = resources;
    }

    void setupContentView(AppCompatActivity activity, Listener listener) {
        ButterKnife.bind(this, activity.findViewById(android.R.id.content));
        setListener(listener);
    }

    private void setListener(final Listener listener) {
        buyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.startPurchase();
            }
        });
    }

    public void showSuccess() {
        upgradeHeader.setVisibility(View.GONE);
        successHeader.setVisibility(View.VISIBLE);
    }

    public void enableBuyButton() {
        buyButton.setEnabled(true);
    }

    public void disableBuyButton() {
        buyButton.setEnabled(false);
    }

    public void showBuyButton(String price) {
        buyButton.setText(resources.getString(R.string.upgrade_buy_price, price));
        loading.setVisibility(View.GONE);
        AnimUtils.showView(buyButton, true);
    }

    public void hideBuyButton() {
        AnimUtils.hideView(buyButton, true);
        loading.setVisibility(View.VISIBLE);
    }

}
