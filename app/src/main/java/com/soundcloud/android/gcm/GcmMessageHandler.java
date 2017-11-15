package com.soundcloud.android.gcm;

import com.google.firebase.messaging.RemoteMessage;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.playback.ConcurrentPlaybackOperations;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.strings.Strings;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GcmMessageHandler {

    private static final String TAG = "GcmMessageReceiver";
    private static final String SC_ACTION_STOP = "stop";
    @VisibleForTesting static final String EXTRA_DATA = "data";

    /** See @{@link com.appboy.push.AppboyNotificationUtils#isAppboyPushMessage(android.content.Intent)}. */
    @VisibleForTesting static final String APPBOY_KEY = "_ab";
    @VisibleForTesting static final String APPBOY_VALUE = "true";

    private final Resources resources;
    private final GcmDecryptor gcmDecryptor;
    private final ConcurrentPlaybackOperations concurrentPlaybackOperations;
    private final AccountOperations accountOperations;
    private final GcmStorage gcmStorage;

    private Listener listener;

    @Inject
    public GcmMessageHandler(Resources resources,
                             GcmDecryptor gcmDecryptor,
                             ConcurrentPlaybackOperations concurrentPlaybackOperations,
                             AccountOperations accountOperations,
                             GcmStorage gcmStorage) {
        this.resources = resources;
        this.gcmDecryptor = gcmDecryptor;
        this.concurrentPlaybackOperations = concurrentPlaybackOperations;
        this.accountOperations = accountOperations;
        this.gcmStorage = gcmStorage;
    }

    public void handleMessage(RemoteMessage remoteMessage) {
        final String scApiKey = resources.getString(R.string.gcm_defaultSenderId);
        if (scApiKey.equals(remoteMessage.getFrom())) {
            final String appboyValue = Strings.nullToEmpty(remoteMessage.getData().get(APPBOY_KEY));
            if (appboyValue.equals(APPBOY_VALUE)) {
                return;
            }

            final String payload = remoteMessage.getData().get(EXTRA_DATA);
            if (Strings.isBlank(payload)) {
                ErrorUtils.handleSilentException(new IllegalArgumentException("Blank Remote Message Payload : " + getDebugString(remoteMessage)));
            } else {
                handleScMessage(remoteMessage, payload);
            }
        }
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private void handleScMessage(RemoteMessage remoteMessage, String payload) {
        try {
            final String decryptedString = gcmDecryptor.decrypt(payload);

            if (listener != null) {
                listener.onRemoteMessage(remoteMessage, decryptedString);
            }

            logScMessage(decryptedString);
            final JSONObject jsonPayload = new JSONObject(decryptedString);
            if (isStopAction(jsonPayload) && isLoggedInUser(jsonPayload) && isNotSelfTriggered(jsonPayload)) {
                if (!jsonPayload.optBoolean("stealth")) {
                    concurrentPlaybackOperations.pauseIfPlaying();
                }
            }
        } catch (Exception e) {
            ErrorUtils.handleSilentException(new IllegalArgumentException("Unable to handle remote message : " + getDebugString(remoteMessage)));
        }
    }


    @NonNull
    private String getDebugString(RemoteMessage remoteMessage) {
        MoreObjects.ToStringHelper toStringHelper = MoreObjects.toStringHelper(remoteMessage);
        toStringHelper.add("data", remoteMessage.getData());
        if (remoteMessage.getNotification() != null) {
            toStringHelper.add("body", remoteMessage.getNotification().getBody());
            toStringHelper.add("title", remoteMessage.getNotification().getTitle());
        }
        return toStringHelper.toString();
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

    private boolean isNotSelfTriggered(JSONObject jsonPayload) throws JSONException {
        final String token = gcmStorage.getToken();
        return token == null || !token.equals(jsonPayload.getString("token"));
    }

    public interface Listener {
        void onRemoteMessage(RemoteMessage remoteMessage, String payload);
    }
}
