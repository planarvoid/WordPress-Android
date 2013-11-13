package com.soundcloud.android.onboarding.suggestions;

import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.ActionBarController;
import com.soundcloud.android.main.ScActivity;

import android.os.Bundle;

public abstract class SuggestedUsersBaseActivity extends ScActivity {

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.container_layout);
    }

    protected ActionBarController createActionBarController() {
        return new ActionBarController(this, mPublicCloudAPI);
    }

}
