package com.soundcloud.android.gcm;

import com.google.android.gms.common.ConnectionResult;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.utils.GooglePlayServicesWrapper;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import javax.inject.Inject;

public class GcmManager extends DefaultActivityLightCycle<AppCompatActivity> {

    private static final String TAG = "GcmHelper";

    private final ApplicationProperties appProperties;
    private final GcmStorage gcmStorage;
    private final GooglePlayServicesWrapper googlePlayServices;

    @Inject
    public GcmManager(ApplicationProperties appProperties,
                      GcmStorage gcmStorage,
                      GooglePlayServicesWrapper googlePlayServices) {

        this.appProperties = appProperties;
        this.gcmStorage = gcmStorage;
        this.googlePlayServices = googlePlayServices;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        super.onCreate(activity, bundle);
        if (bundle == null) {
            int resultCode = googlePlayServices.getPlayServicesAvailableStatus(activity);
            if (resultCode == ConnectionResult.SUCCESS) {
                ensureRegistration(activity);
            } else {
                handlePlayServicesUnavailable(activity, resultCode);
            }
        }
    }

    private void ensureRegistration(AppCompatActivity activity) {
        if (gcmStorage.shouldRegister()){
            GcmRegistrationService.startGcmService(activity);
        }
    }

    private void handlePlayServicesUnavailable(AppCompatActivity activity, int resultCode) {
        if (googlePlayServices.isUserRecoverableError(resultCode)) {
            if (appProperties.isGooglePlusEnabled()) {
                googlePlayServices.showUnrecoverableErrorDialog(activity, resultCode);
            }
        } else {
            Log.d(TAG, "This device is not supported.");
        }
    }
}
