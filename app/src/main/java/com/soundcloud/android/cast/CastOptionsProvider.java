package com.soundcloud.android.cast;

import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;
import com.google.android.gms.cast.framework.media.CastMediaOptions;
import com.google.android.gms.cast.framework.media.NotificationOptions;
import com.soundcloud.android.R;
import com.soundcloud.android.main.MainActivity;

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
                .setCastMediaOptions(castMediaOptions())
                .build();
    }

    private CastMediaOptions castMediaOptions() {
        NotificationOptions notificationOptions = new NotificationOptions.Builder()
                .setTargetActivityClassName(CastRedirectActivity.class.getName())
                .build();

        return new CastMediaOptions.Builder()
                .setNotificationOptions(notificationOptions)
                .build();
    }

    @Override
    public List<SessionProvider> getAdditionalSessionProviders(Context context) {
        return null;
    }
}
