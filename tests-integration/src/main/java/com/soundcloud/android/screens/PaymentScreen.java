package com.soundcloud.android.screens;

import com.soundcloud.android.payments.PaymentsActivity;
import com.soundcloud.android.tests.Han;

public class PaymentScreen extends Screen {
    private static final Class ACTIVITY = PaymentsActivity.class;

    public PaymentScreen(Han solo) {
        super(solo);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

}
