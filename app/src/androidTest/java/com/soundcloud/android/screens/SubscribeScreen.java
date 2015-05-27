package com.soundcloud.android.screens;

import com.soundcloud.android.payments.SubscribeActivity;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.R;

public class SubscribeScreen extends Screen {

    private static final Class ACTIVITY = SubscribeActivity.class;

    public SubscribeScreen(Han solo) {
        super(solo);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public SubscribeSuccessScreen clickBuyForSuccess() {
        clickBuy();
        return new SubscribeSuccessScreen(testDriver);
    }

    public PaymentErrorScreen clickBuyForFailure() {
        clickBuy();
        return new PaymentErrorScreen(testDriver);
    }

    private SubscribeScreen clickBuy() {
        waiter.waitForElement(R.id.subscribe_buy);
        testDriver.clickOnButtonWithText(R.string.subscribe_buy);
        return this;
    }

}
