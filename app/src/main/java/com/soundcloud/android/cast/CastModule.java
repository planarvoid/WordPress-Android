package com.soundcloud.android.cast;

import com.google.android.gms.cast.framework.CastContext;
import com.soundcloud.android.cast.api.CastProtocol;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.utils.GooglePlayServicesWrapper;
import com.soundcloud.rx.eventbus.EventBus;
import dagger.Lazy;
import dagger.Module;
import dagger.Provides;

import android.content.Context;

import javax.inject.Singleton;

@Module
public class CastModule {

    private static final int REQUIRED_GOOGLE_PLAY_SERVICE_VERSION = 9256000;

    @Provides
    @Singleton
    public CastContextWrapper provideCastContext(GooglePlayServicesWrapper googlePlayServicesWrapper, Context context) {
        try {
            if (isCastEnabled(googlePlayServicesWrapper, context)) {
                return new DefaultCastContextWrapper(CastContext.getSharedInstance(context));
            } else {
                return new NoOpCastContextWrapper();
            }
        } catch (Exception exception) {
            return new NoOpCastContextWrapper();
        }
    }

    @Provides
    @Singleton
    public CastConnectionHelper provideCastConnectionHelper(Context context,
                                                            Lazy<CastContextWrapper> castContext,
                                                            GooglePlayServicesWrapper gpsWrapper) {
        // The dalvik switch is a horrible hack to prevent instantiation of the real cast manager in unit tests as it crashes on robolectric.
        // This is temporary, until we play https://soundcloud.atlassian.net/browse/MC-213
        if ("Dalvik".equals(System.getProperty("java.vm.name"))) {
            if (isCastEnabled(gpsWrapper, context)) {
                return new DefaultCastConnectionHelper(castContext.get());
            }
        }
        return new NoOpCastConnectionHelper();
    }

    @Provides
    @Singleton
    public CastPlayer provideCastPlayer(PlayQueueManager playQueueManager,
                                        EventBus eventBus,
                                        CastProtocol castProtocol,
                                        PlaySessionStateProvider playSessionStateProvider,
                                        CastQueueController castQueueController,
                                        CastPlayStateReporter castPlayStateReporter,
                                        CastQueueSlicer castQueueSlicer) {
        return new DefaultCastPlayer(playQueueManager, eventBus,
                                     castProtocol, playSessionStateProvider,
                                     castQueueController, castPlayStateReporter, castQueueSlicer);
    }

    private boolean isCastEnabled(GooglePlayServicesWrapper googlePlayServicesWrapper, Context context) {
        return googlePlayServicesWrapper.isPlayServiceAvailable(context, REQUIRED_GOOGLE_PLAY_SERVICE_VERSION);
    }

}
