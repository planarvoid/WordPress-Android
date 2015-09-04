package com.soundcloud.android.gcm;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Log;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;

import javax.inject.Inject;

public class GcmMessageHandler {

    private static final String TAG = "GcmMessageReceiver";
    private static final String RECEIVE_MESSSAGE_ACTION = "com.google.android.c2dm.intent.RECEIVE";
    private static final String SC_ACTION_STOP = "stop";

    private static final String EXTRA_FROM = "from";
    private static final String EXTRA_DATA = "data";

    private final Resources resources;
    private final FeatureFlags featureFlags;
    private final GcmDecryptor gcmDecryptor;
    private final PlaySessionController playSessionController;

    @Inject
    public GcmMessageHandler(Resources resources, FeatureFlags featureFlags, GcmDecryptor gcmDecryptor,
                             PlaySessionController playSessionController) {
        this.resources = resources;
        this.featureFlags = featureFlags;
        this.gcmDecryptor = gcmDecryptor;
        this.playSessionController = playSessionController;
    }

    public void handleMessage(Intent intent) {
        if (RECEIVE_MESSSAGE_ACTION.equals(intent.getAction())) {
            final String scApiKey = resources.getString(R.string.google_api_key);
            if (scApiKey.equals(intent.getStringExtra(EXTRA_FROM))) {
                handleScMessage(intent.getStringExtra(EXTRA_DATA));
            }
        }
    }

    private void handleScMessage(String payload) {
        try {
            final String decryptedString = gcmDecryptor.decrypt(payload);
            Log.i(TAG, "Received SC Message : " + decryptedString);
            final JSONObject jsonPayload = new JSONObject(decryptedString);
            if (featureFlags.isEnabled(Flag.KILL_CONCURRENT_STREAMING) &&
                    SC_ACTION_STOP.equals(jsonPayload.getString("action"))){
                playSessionController.pause();
            }

        } catch (Exception e) {
            ErrorUtils.handleSilentException(e, "payload", payload);
        }
    }
}
