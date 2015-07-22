package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
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
        waiter.waitForElement(R.id.upgrade_buy);
        testDriver.clickOnView(With.id(R.id.upgrade_buy));
        return this;
    }

    public boolean isDisplayingSuccess() {
        return testDriver.findElement(With.id(R.id.success_header)).isVisible();
    }

}
