package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.payments.UpgradeActivity;

public class PaymentErrorScreen extends Screen {

    public PaymentErrorScreen(Han solo) {
        super(solo);
    }

    @Override
    protected Class getActivity() {
        return UpgradeActivity.class;
    }

    public boolean waitForDialog() {
        return waiter.waitForFragmentByTag("payment_error");
    }

    public String getMessage() {
        return new TextElement(testDriver.findOnScreenElement(With.id(R.id.custom_dialog_body))).getText();
    }

}
