package com.soundcloud.android.cast.activity;

import com.soundcloud.android.Navigator;

import android.content.Intent;

public class CastExpandedControllerRedirectActivity extends CastRedirectActivity {
    @Override
    protected Intent getRedirectionIntent(Navigator navigator) {
        return navigator.createHomeIntentFromCastExpandedController(this);
    }
}
