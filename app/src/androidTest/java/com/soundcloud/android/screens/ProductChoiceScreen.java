package com.soundcloud.android.screens;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.payments.ProductChoiceActivity;

public class ProductChoiceScreen extends Screen {

    private static final Class ACTIVITY = ProductChoiceActivity.class;

    ProductChoiceScreen(Han solo) {
        super(solo);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public WebCheckoutScreen clickBuyButton() {
        upgradeButton().click();
        return new WebCheckoutScreen(testDriver);
    }

    public ProductChoiceScreen swipeToHighTierPlan() {
        testDriver.swipeLeft();
        return this;
    }

    private ViewElement upgradeButton() {
        return testDriver.findOnScreenElement(With.id(R.id.buy));
    }

}
