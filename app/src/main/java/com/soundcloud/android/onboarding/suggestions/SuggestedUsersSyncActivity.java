package com.soundcloud.android.onboarding.suggestions;

import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.ActionBarController;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.android.main.ScActivity;

import android.os.Bundle;
import android.view.Menu;

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.onboard, menu);
        return true;
    }
}
