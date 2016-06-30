package com.soundcloud.android.gcm;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.utils.Log;

import javax.inject.Inject;

public class ScFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FirebaseMessaging";

    @Inject GcmMessageHandler gcmMessageHandler;

    public ScFirebaseMessagingService() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }


    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "Received Remote Message: " + remoteMessage);
        gcmMessageHandler.handleMessage(remoteMessage);
    }
}
