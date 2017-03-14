package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.payments.ConversionActivity;

public class UpgradeScreen extends Screen {

    private static final Class ACTIVITY = ConversionActivity.class;

    public UpgradeScreen(Han solo) {
        super(solo);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public WebCheckoutScreen clickBuyForWebCheckout() {
        clickBuy();
        return new WebCheckoutScreen(testDriver);
    }

    private UpgradeScreen clickBuy() {
        ViewElement button = upgradeButton();
        waiter.waitForAnimationToFinish(button);
        button.click();
        return this;
    }

    public ViewElement upgradeButton() {
        return testDriver.findOnScreenElement(With.id(R.id.conversion_buy));
    }

}
