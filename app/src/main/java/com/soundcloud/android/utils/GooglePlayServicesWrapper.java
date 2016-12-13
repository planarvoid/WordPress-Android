package com.soundcloud.android.utils;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import android.app.Activity;
import android.content.Context;

import javax.inject.Inject;

public class GooglePlayServicesWrapper {

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000; // magic number from the samples

    @Inject
    public GooglePlayServicesWrapper() {
    }

    public int getPlayServicesAvailableStatus(Context context) {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
    }

    public boolean isPlayServiceAvailable(Context context) {
        try {
            return getPlayServicesAvailableStatus(context) == ConnectionResult.SUCCESS;
        } catch (Exception exception) {
            return false;
        }
    }

    public boolean isPlayServiceAvailable(Context context, int requiredVersion) {
        return isPlayServiceAvailable(context) && GoogleApiAvailability.GOOGLE_PLAY_SERVICES_VERSION_CODE >= requiredVersion;
    }

    public boolean isUserRecoverableError(int resultCode) {
        return GoogleApiAvailability.getInstance().isUserResolvableError(resultCode);
    }

    public void showUnrecoverableErrorDialog(Activity activity, int resultCode) {
        GoogleApiAvailability.getInstance().getErrorDialog(activity, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
    }
}
