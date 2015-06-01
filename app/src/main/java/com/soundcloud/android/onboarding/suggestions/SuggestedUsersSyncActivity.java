package com.soundcloud.android.onboarding.suggestions;

import com.soundcloud.android.actionbar.ActionBarController;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.lightcycle.LightCycle;

import android.os.Bundle;

import javax.inject.Inject;

public class SuggestedUsersSyncActivity extends ScActivity {
    @Inject @LightCycle ActionBarController actionBarController;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (state == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(getContentHolderViewId(), new OnboardSuggestedUsersSyncFragment())
                    .commit();
        }
    }

}
