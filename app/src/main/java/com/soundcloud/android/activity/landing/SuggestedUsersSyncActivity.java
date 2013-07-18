package com.soundcloud.android.activity.landing;

import com.soundcloud.android.R;
import com.soundcloud.android.dialog.OnboardSuggestedUsersSyncFragment;

import android.os.Bundle;

public class SuggestedUsersSyncActivity extends SuggestedUsersBaseActivity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (state == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.holder, new OnboardSuggestedUsersSyncFragment())
                    .commit();
        }
    }

    @Override
    public int getMenuResourceId() {
        return R.menu.onboard;
    }
}
