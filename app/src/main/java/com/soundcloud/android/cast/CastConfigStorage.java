package com.soundcloud.android.cast;

import com.soundcloud.android.R;
import com.soundcloud.android.properties.ApplicationProperties;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Inject;

public class CastConfigStorage {

    private static final String RECEIVER_ID_OVERRIDE = "receiver_id_override";

    private final Context context;
    private final SharedPreferences sharedPreferences;
    private final ApplicationProperties applicationProperties;

    @Inject
    CastConfigStorage(Context context, SharedPreferences sharedPreferences,
                      ApplicationProperties applicationProperties) {
        this.context = context;
        this.sharedPreferences = sharedPreferences;
        this.applicationProperties = applicationProperties;
    }

    private String getDefaultReceiverID() {
        return context.getString(R.string.cast_receiver_app_id);
    }

    public String getReceiverID() {
        if (applicationProperties.isReleaseBuild()) {
            return getDefaultReceiverID();
        } else {
            return sharedPreferences.getString(RECEIVER_ID_OVERRIDE, getDefaultReceiverID());
        }
    }

    public void saveReceiverIDOverride(String newID) {
        sharedPreferences.edit().putString(RECEIVER_ID_OVERRIDE, newID).apply();
    }

    public void reset() {
        sharedPreferences.edit().remove(RECEIVER_ID_OVERRIDE).apply();
    }
}
