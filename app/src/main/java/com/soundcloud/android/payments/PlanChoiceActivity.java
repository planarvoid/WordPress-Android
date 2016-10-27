package com.soundcloud.android.payments;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.LoggedInActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.lightcycle.LightCycle;

import javax.inject.Inject;

public class PlanChoiceActivity extends LoggedInActivity {

    static final String AVAILABLE_PRODUCTS = "available_products";

    @Inject @LightCycle PlanChoicePresenter presenter;

    public PlanChoiceActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public Screen getScreen() {
        // TODO: separate screen ID for plan choice
        return Screen.CONVERSION;
    }

    @Override
    protected void setActivityContentView() {
        super.setContentView(R.layout.plan_choice_activity);
    }

    @Override
    protected boolean receiveConfigurationUpdates() {
        return false;
    }

}
