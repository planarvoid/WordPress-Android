package com.soundcloud.android.screens;

import com.soundcloud.android.payments.SubscribeActivity;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.R;

public class PaymentScreen extends Screen {
    private static final Class ACTIVITY = SubscribeActivity.class;

    public PaymentScreen(Han solo) {
        super(solo);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public void clickBuy() {
        waiter.waitForElement(R.id.subscribe_buy);
        testDriver.clickOnButtonWithText(R.string.subscribe_buy);
    }

}
