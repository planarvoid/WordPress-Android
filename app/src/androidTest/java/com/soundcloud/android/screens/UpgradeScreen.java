package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.payments.UpgradeActivity;

public class UpgradeScreen extends Screen {

    private static final Class ACTIVITY = UpgradeActivity.class;

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

    private UpgradeScreen clickBuy() {
        upgradeButton().click();
        return this;
    }

    public ViewElement upgradeButton() {
        return testDriver.findOnScreenElement(With.id(R.id.upgrade_buy));
    }

    public boolean isDisplayingSuccess() {
        return testDriver.findOnScreenElement(With.id(R.id.success_header)).isOnScreen();
    }

}
