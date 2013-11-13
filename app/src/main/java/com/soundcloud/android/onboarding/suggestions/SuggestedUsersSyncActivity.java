package com.soundcloud.android.onboarding.suggestions;

import com.soundcloud.android.R;

import android.os.Bundle;

public class SuggestedUsersSyncActivity extends SuggestedUsersBaseActivity {
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
