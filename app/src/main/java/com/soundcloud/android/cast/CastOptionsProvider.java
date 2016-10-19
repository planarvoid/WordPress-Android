package com.soundcloud.android.cast;

import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;
import com.soundcloud.android.R;

import android.content.Context;

import java.util.List;

@SuppressWarnings("unused")
public class CastOptionsProvider implements OptionsProvider {

    @Override
    public CastOptions getCastOptions(Context context) {
        return new CastOptions.Builder()
                .setReceiverApplicationId(context.getString(R.string.cast_receiver_app_id))
                .setEnableReconnectionService(true)
                .setResumeSavedSession(true)
                .build();

        // TODO: Missing notification, debug, lockscreen, targetactivity
    }

    @Override
    public List<SessionProvider> getAdditionalSessionProviders(Context context) {
        return null;
    }
}
