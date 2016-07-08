package com.soundcloud.android.gcm;

import com.google.android.gms.iid.InstanceIDListenerService;
import com.soundcloud.android.SoundCloudApplication;

import javax.inject.Inject;

public class GcmInstanceIDListenerService extends InstanceIDListenerService {

    @Inject GcmStorage gcmStorage;

    public GcmInstanceIDListenerService() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. This call is initiated by the
     * InstanceID provider.
     */
    @Override
    public void onTokenRefresh() {
        gcmStorage.clearHasRegistered();
        GcmRegistrationService.startGcmService(this);
    }
}
