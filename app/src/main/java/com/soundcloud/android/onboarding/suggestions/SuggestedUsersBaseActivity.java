package com.soundcloud.android.onboarding.suggestions;

import com.soundcloud.android.actionbar.ActionBarController;
import com.soundcloud.android.main.ScActivity;

public abstract class SuggestedUsersBaseActivity extends ScActivity {

    protected ActionBarController createActionBarController() {
        return new ActionBarController(this, eventBus);
    }

}
