package com.soundcloud.android.cast;

import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;
import com.google.android.gms.cast.framework.media.CastMediaOptions;
import com.google.android.gms.cast.framework.media.NotificationOptions;
import com.soundcloud.android.SoundCloudApplication;

import android.content.Context;

import javax.inject.Inject;
import java.util.List;

@SuppressWarnings("unused")
public class CastOptionsProvider implements OptionsProvider {

    @Inject CastConfigStorage castConfigStorage;

    public CastOptionsProvider() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public CastOptions getCastOptions(Context context) {
        return new CastOptions.Builder()
                .setReceiverApplicationId(castConfigStorage.getReceiverID())
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
