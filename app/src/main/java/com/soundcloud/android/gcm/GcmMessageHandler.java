package com.soundcloud.android.gcm;

import com.google.firebase.messaging.RemoteMessage;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.playback.ConcurrentPlaybackOperations;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.strings.Strings;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.res.Resources;

import javax.inject.Inject;

public class GcmMessageHandler {

    private static final String TAG = "GcmMessageReceiver";
    private static final String RECEIVE_MESSSAGE_ACTION = "com.google.android.c2dm.intent.RECEIVE";
    private static final String SC_ACTION_STOP = "stop";

    private static final String EXTRA_FROM = "from";
    private static final String EXTRA_DATA = "data";

    private final Resources resources;
    private final GcmDecryptor gcmDecryptor;
    private final ConcurrentPlaybackOperations concurrentPlaybackOperations;
    private final AccountOperations accountOperations;

    @Inject
    public GcmMessageHandler(Resources resources,
                             GcmDecryptor gcmDecryptor,
                             ConcurrentPlaybackOperations concurrentPlaybackOperations,
                             AccountOperations accountOperations) {
        this.resources = resources;
        this.gcmDecryptor = gcmDecryptor;
        this.concurrentPlaybackOperations = concurrentPlaybackOperations;
        this.accountOperations = accountOperations;
    }

    public void handleMessage(RemoteMessage remoteMessage) {
        final String scApiKey = resources.getString(R.string.gcm_defaultSenderId);
        if (scApiKey.equals(remoteMessage.getFrom())) {
            final String payload = remoteMessage.getData().get(EXTRA_DATA);
            if (Strings.isBlank(payload)) {
                ErrorUtils.handleSilentException(new IllegalArgumentException("Blank Remote Message Payload : " + remoteMessage));
            } else {
                handleScMessage(payload);
            }
        }
    }

    private void handleScMessage(String payload) {
        try {
            final String decryptedString = gcmDecryptor.decrypt(payload);
            logScMessage(decryptedString);
            final JSONObject jsonPayload = new JSONObject(decryptedString);
            if (isStopAction(jsonPayload) && isLoggedInUser(jsonPayload)) {
                if (!jsonPayload.optBoolean("stealth")) {
                    concurrentPlaybackOperations.pauseIfPlaying();
                }
            }
        } catch (Exception e) {
            ErrorUtils.handleSilentException(e, "payload", payload);
        }
    }

    private void logScMessage(String decryptedString) {
        Log.d(TAG, "Received SC Message : " + decryptedString);
    }

    private boolean isLoggedInUser(JSONObject jsonPayload) throws JSONException {
        return accountOperations.getLoggedInUserUrn().getNumericId() == jsonPayload.getLong("user_id");
    }

    private boolean isStopAction(JSONObject jsonPayload) throws JSONException {
        return SC_ACTION_STOP.equals(jsonPayload.getString("action"));
    }
}
