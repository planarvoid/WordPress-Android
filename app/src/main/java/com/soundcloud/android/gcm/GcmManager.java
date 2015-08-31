package com.soundcloud.android.gcm;

import com.google.android.gms.common.ConnectionResult;
import com.soundcloud.android.ServiceInitiator;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.utils.GooglePlayServicesWrapper;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import javax.inject.Inject;

public class GcmManager extends DefaultActivityLightCycle<AppCompatActivity> {

    private static final String TAG = "GcmHelper";

    private final GcmStorage gcmStorage;
    private final GooglePlayServicesWrapper googlePlayServices;
    private final ServiceInitiator serviceInitiator;
    private final FeatureFlags featureFlags;

    @Inject
    public GcmManager(GcmStorage gcmStorage, GooglePlayServicesWrapper googlePlayServices, ServiceInitiator serviceInitiator, FeatureFlags featureFlags) {
        this.gcmStorage = gcmStorage;
        this.googlePlayServices = googlePlayServices;
        this.serviceInitiator = serviceInitiator;
        this.featureFlags = featureFlags;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        super.onCreate(activity, bundle);

        if (featureFlags.isEnabled(Flag.KILL_CONCURRENT_STREAMING) && bundle == null) {
            int resultCode = googlePlayServices.isPlayServicesAvailable(activity);
            if (resultCode == ConnectionResult.SUCCESS) {
                ensureRegistrationTokenStored(activity);
            } else {
                handlePlayServicesUnavailable(activity, resultCode);
            }
        }
    }

    private void ensureRegistrationTokenStored(AppCompatActivity activity) {
        if (!gcmStorage.hasToken()) {
            serviceInitiator.startGcmService(activity);
        } else {
            Log.d(TAG, "GcmToken found " + gcmStorage.getToken());
        }
    }

    private void handlePlayServicesUnavailable(AppCompatActivity activity, int resultCode) {
        if (googlePlayServices.isUserRecoverableError(resultCode)) {
            googlePlayServices.showUnrecoverableErrorDialog(activity, resultCode);
        } else {
            Log.d(TAG, "This device is not supported.");
        }
    }
}
