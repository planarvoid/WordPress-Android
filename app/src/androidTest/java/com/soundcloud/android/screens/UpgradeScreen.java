package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.payments.WebConversionActivity;

public class UpgradeScreen extends Screen {

    private static final Class ACTIVITY = WebConversionActivity.class;

    public UpgradeScreen(Han solo) {
        super(solo);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public UpgradeScreen clickBuyForSuccess() {
        clickBuy();
        return this;
    }

    public PaymentErrorScreen clickBuyForFailure() {
        clickBuy();
        return new PaymentErrorScreen(testDriver);
    }

    public WebCheckoutScreen clickBuyForWebCheckout() {
        clickBuy();
        return new WebCheckoutScreen(testDriver);
    }

    private UpgradeScreen clickBuy() {
        upgradeButton().click();
        return this;
    }

    public ViewElement upgradeButton() {
        return testDriver.findOnScreenElement(With.id(R.id.conversion_buy));
    }

}
