package com.soundcloud.android.cast;

import com.soundcloud.android.R;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Inject;

public class CastConfigStorage {

    private static final String RECEIVER_ID_OVERRIDE = "receiver_id_override";

    private final Context context;
    private final SharedPreferences sharedPreferences;
    private final ApplicationProperties applicationProperties;
    private final FeatureFlags featureFlags;

    @Inject
    CastConfigStorage(Context context, SharedPreferences sharedPreferences,
                      ApplicationProperties applicationProperties,
                      FeatureFlags featureFlags) {
        this.context = context;
        this.sharedPreferences = sharedPreferences;
        this.applicationProperties = applicationProperties;
        this.featureFlags = featureFlags;
    }

    private String getDefaultReceiverID() {
        if (featureFlags.isEnabled(Flag.CAST_V3)) {
            return context.getString(R.string.cast_v3_receiver_app_id);
        } else {
            return context.getString(R.string.cast_receiver_app_id);
        }
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
