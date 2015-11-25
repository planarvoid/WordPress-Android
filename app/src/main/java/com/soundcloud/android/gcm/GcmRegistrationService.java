package com.soundcloud.android.gcm;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.appboy.AppboyWrapper;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import javax.inject.Inject;
import javax.inject.Provider;

public class GcmRegistrationService extends IntentService {

    private static final String TAG = "GcmRegistrationService";

    @Inject GcmStorage gcmStorage;
    @Inject InstanceIdWrapper instanceId;
    @Inject Provider<AppboyWrapper> appboyWrapperProvider;

    public GcmRegistrationService() {
        super(TAG);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    GcmRegistrationService(GcmStorage gcmStorage,
                           InstanceIdWrapper instanceId,
                           Provider<AppboyWrapper> appboyWrapperProvider) {
        super(TAG);
        this.gcmStorage = gcmStorage;
        this.instanceId = instanceId;
        this.appboyWrapperProvider = appboyWrapperProvider;
    }

    @Override
    protected void onHandleIntent(Intent ignored) {
        try {
            String token = instanceId.getToken(this, getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE);

            Log.d(TAG, "GCM Registration Token: " + token);

            appboyWrapperProvider.get().handleRegistration(token);

            gcmStorage.storeToken(token);

        } catch (Exception e) {
            Log.d(TAG, "Failed to complete token refresh", e);
            gcmStorage.clearToken();
        }
    }
}
