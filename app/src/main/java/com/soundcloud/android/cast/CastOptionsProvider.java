package com.soundcloud.android.cast;

import com.google.android.gms.cast.LaunchOptions;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;
import com.google.android.gms.cast.framework.media.CastMediaOptions;
import com.google.android.gms.cast.framework.media.MediaIntentReceiver;
import com.google.android.gms.cast.framework.media.NotificationOptions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;

import android.content.Context;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
                .setLaunchOptions(launchOptions())
                .build();
    }

    private LaunchOptions launchOptions() {
        return new LaunchOptions.Builder()
                .setLocale(Locale.getDefault())
                .build();
    }

    private CastMediaOptions castMediaOptions() {
        List<String> buttonActions = new ArrayList<>();
        buttonActions.add(MediaIntentReceiver.ACTION_REWIND);
        buttonActions.add(MediaIntentReceiver.ACTION_TOGGLE_PLAYBACK);
        buttonActions.add(MediaIntentReceiver.ACTION_FORWARD);
        buttonActions.add(MediaIntentReceiver.ACTION_STOP_CASTING);
        int[] compatButtonActionsIndicies = new int[]{
                buttonActions.indexOf(MediaIntentReceiver.ACTION_TOGGLE_PLAYBACK), buttonActions.indexOf(MediaIntentReceiver.ACTION_STOP_CASTING)
        };


        NotificationOptions notificationOptions = new NotificationOptions.Builder()
                .setActions(buttonActions, compatButtonActionsIndicies)
                .setSmallIconDrawableResId(R.drawable.ic_notification_cloud)
                .setForward10DrawableResId(R.drawable.notifications_next)
                .setForward30DrawableResId(R.drawable.notifications_next)
                .setForwardDrawableResId(R.drawable.notifications_next)
                .setSkipNextDrawableResId(R.drawable.notifications_next)
                .setSkipPrevDrawableResId(R.drawable.notifications_previous)
                .setRewind10DrawableResId(R.drawable.notifications_previous)
                .setRewind30DrawableResId(R.drawable.notifications_previous)
                .setRewindDrawableResId(R.drawable.notifications_previous)
                .setTargetActivityClassName(CastRedirectActivity.class.getName())
                .build();

        return new CastMediaOptions.Builder()
                .setNotificationOptions(notificationOptions)
                .setImagePicker(new CastImagePicker())
                .setMediaIntentReceiverClassName(CastMediaIntentReceiver.class.getName())
                .build();
    }

    @Override
    public List<SessionProvider> getAdditionalSessionProviders(Context context) {
        return null;
    }
}
