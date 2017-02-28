package com.soundcloud.android.cast.activity;

import com.soundcloud.android.Navigator;

import android.content.Intent;

public class CastNotificationRedirectActivity extends CastRedirectActivity {
    @Override
    protected Intent getRedirectionIntent(Navigator navigator) {
        return navigator.createHomeIntentFromNotification(this);
    }
}
