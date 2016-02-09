package com.soundcloud.android.payments;

import com.soundcloud.android.R;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.lightcycle.LightCycle;

import javax.inject.Inject;

public class WebCheckoutActivity extends ScActivity {
    @Inject BaseLayoutHelper baseLayoutHelper;
    @Inject @LightCycle WebCheckoutPresenter presenter;

    @Override
    protected void setActivityContentView() {
        super.setContentView(R.layout.web_checkout_activity);
        baseLayoutHelper.setupActionBar(this);
    }

    @Override
    public Screen getScreen() {
        return Screen.CHECKOUT;
    }

    @Override
    public void onBackPressed() {
        if (!presenter.handleBackPress()) {
            super.onBackPressed();
        }
    }
}
