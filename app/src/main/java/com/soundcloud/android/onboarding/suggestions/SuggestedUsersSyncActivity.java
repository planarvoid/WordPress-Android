package com.soundcloud.android.onboarding.suggestions;

import com.soundcloud.android.R;
import com.soundcloud.android.main.ScActivity;

import android.os.Bundle;

public class SuggestedUsersSyncActivity extends ScActivity {
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
    public int getMenuResourceId() {
        return R.menu.onboard;
    }
}
