package com.soundcloud.android.screens;

import com.soundcloud.android.payments.SubscribeActivity;
import com.soundcloud.android.tests.Han;

public class PaymentScreen extends Screen {
    private static final Class ACTIVITY = SubscribeActivity.class;

    public PaymentScreen(Han solo) {
        super(solo);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

}
