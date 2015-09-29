package com.soundcloud.android.gcm;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.appboy.AppboyWrapper;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import javax.inject.Inject;
import javax.inject.Provider;

public class GcmRegistrationService extends IntentService {

    private static final String TAG = "GcmRegistrationService";
    private static final String[] TOPICS = {"global"};

    @Inject GcmStorage gcmStorage;
    @Inject InstanceIdWrapper instanceId;
    @Inject FeatureFlags featureFlags;
    @Inject Provider<AppboyWrapper> appboyWrapperProvider;

    public GcmRegistrationService() {
        super(TAG);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    GcmRegistrationService(GcmStorage gcmStorage,
                           InstanceIdWrapper instanceId,
                           FeatureFlags featureFlags,
                           Provider<AppboyWrapper> appboyWrapperProvider) {
        super(TAG);
        this.gcmStorage = gcmStorage;
        this.instanceId = instanceId;
        this.featureFlags = featureFlags;
        this.appboyWrapperProvider = appboyWrapperProvider;
    }

    @Override
    protected void onHandleIntent(Intent ignored) {
        try {
            String token = instanceId.getToken(this, getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE);

            Log.d(TAG, "GCM Registration Token: " + token);

            if (featureFlags.isEnabled(Flag.APPBOY)) {
                appboyWrapperProvider.get().handleRegistration(token);
            }

            gcmStorage.storeToken(token);

        } catch (Exception e) {
            Log.d(TAG, "Failed to complete token refresh", e);
            gcmStorage.clearToken();
        }
    }
}
