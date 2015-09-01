package com.soundcloud.android.utils;

import com.google.android.gms.common.GooglePlayServicesUtil;

import android.app.Activity;
import android.content.Context;

import javax.inject.Inject;

public class GooglePlayServicesWrapper {

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000; // magic number from the samples

    @Inject
    public GooglePlayServicesWrapper() {
    }

    public int isPlayServicesAvailable(Context context) {
        return GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
    }

    public boolean isUserRecoverableError(int resultCode) {
        return GooglePlayServicesUtil.isUserRecoverableError(resultCode);
    }

    public void showUnrecoverableErrorDialog(Activity activity, int resultCode){
        GooglePlayServicesUtil.getErrorDialog(resultCode, activity, PLAY_SERVICES_RESOLUTION_REQUEST).show();
    }
}
