package com.soundcloud.android.di.testimplementations;

import com.soundcloud.android.utils.GooglePlayServicesWrapper;

import android.app.Activity;

public class NoopGooglePlayServicesWrapper extends GooglePlayServicesWrapper {
    @Override
    public void showUnrecoverableErrorDialog(Activity activity, int resultCode) {
        // noop
    }
}
