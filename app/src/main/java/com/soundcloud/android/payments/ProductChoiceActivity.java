package com.soundcloud.android.payments;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.LoggedInActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.lightcycle.LightCycle;

import javax.inject.Inject;

public class ProductChoiceActivity extends LoggedInActivity {

    static final String AVAILABLE_PRODUCTS = "available_products";

    @Inject @LightCycle ProductChoicePresenter presenter;

    public ProductChoiceActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public Screen getScreen() {
        return Screen.PLAN_CHOICE;
    }

    @Override
    protected void setActivityContentView() {
        super.setContentView(R.layout.product_choice_activity);
    }

    @Override
    protected boolean receiveConfigurationUpdates() {
        return false;
    }

}
