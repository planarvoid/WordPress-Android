package com.soundcloud.android.screens;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.payments.WebCheckoutActivity;

public class WebCheckoutScreen extends Screen {

    private static final Class ACTIVITY = WebCheckoutActivity.class;

    public WebCheckoutScreen(Han solo) {
        super(solo);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

}
