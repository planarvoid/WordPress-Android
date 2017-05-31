package com.soundcloud.android.cast.activity;

import com.soundcloud.android.navigation.IntentFactory;

import android.content.Intent;

public class CastNotificationRedirectActivity extends CastRedirectActivity {
    @Override
    protected Intent getRedirectionIntent() {
        return IntentFactory.createHomeIntentFromNotification(this);
    }
}
