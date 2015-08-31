package com.soundcloud.android.gcm;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import javax.inject.Inject;

public class GcmRegistrationService extends IntentService {

    private static final String TAG = "GcmRegistrationService";
    private static final String[] TOPICS = {"global"};

    @Inject GcmStorage gcmStorage;
    @Inject InstanceIdWrapper instanceId;

    public GcmRegistrationService() {
        super(TAG);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    GcmRegistrationService(GcmStorage gcmStorage, InstanceIdWrapper instanceId) {
        super(TAG);
        this.gcmStorage = gcmStorage;
        this.instanceId = instanceId;
    }

    @Override
    protected void onHandleIntent(Intent ignored) {
        try {
            String token = instanceId.getToken(this, getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE);

            Log.d(TAG, "GCM Registration Token: " + token);

            gcmStorage.storeToken(token);

        } catch (Exception e) {
            Log.d(TAG, "Failed to complete token refresh", e);
            gcmStorage.clearToken();
        }
    }
}
