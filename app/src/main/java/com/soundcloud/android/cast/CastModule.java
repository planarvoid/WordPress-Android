package com.soundcloud.android.cast;

import com.google.android.gms.cast.framework.CastContext;
import com.google.android.libraries.cast.companionlibrary.cast.CastConfiguration;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlayStatePublisher;
import com.soundcloud.android.playback.ProgressReporter;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.GooglePlayServicesWrapper;
import com.soundcloud.rx.eventbus.EventBus;
import dagger.Lazy;
import dagger.Module;
import dagger.Provides;

import android.content.Context;

import javax.inject.Provider;
import javax.inject.Singleton;

@Module
public class CastModule {

    @Provides
    @Singleton
    public CastContextWrapper provideCastContext(GooglePlayServicesWrapper googlePlayServicesWrapper,
                                                 FeatureFlags flags,
                                                 Context context) {
        if (flags.isEnabled(Flag.CAST_V3) && googlePlayServicesWrapper.isPlayServiceAvailable(context)) {
            return new DefaultCastContextWrapper(CastContext.getSharedInstance(context));
        } else {
            return new NoOpCastContextWrapper();
        }
    }

    @Provides
    @Singleton
    public CastConnectionHelper provideCastConnectionHelper(Context context,
                                                            GooglePlayServicesWrapper gpsWrapper,
                                                            CastConfigStorage castConfigStorage,
                                                            FeatureFlags featureFlags) {
        // The dalvik switch is a horrible hack to prevent instantiation of the real cast manager in unit tests as it crashes on robolectric.
        // This is temporary, until we play https://soundcloud.atlassian.net/browse/MC-213
        if ("Dalvik".equals(System.getProperty("java.vm.name"))) {
            if (featureFlags.isDisabled(Flag.CAST_V3)) {
                return new LegacyCastConnectionHelper(provideVideoCastManager(context, castConfigStorage));
            } else if (gpsWrapper.isPlayServiceAvailable(context)) {
                return new DefaultCastConnectionHelper();
            }
        }
        return new NoOpCastConnectionHelper();
    }

    @Provides
    @Singleton
    public VideoCastManager provideVideoCastManager(Context context, CastConfigStorage castConfigStorage) {
        return VideoCastManager
                .initialize(
                        context,
                        new CastConfiguration.Builder(castConfigStorage.getReceiverID())
                                .setTargetActivity(MainActivity.class)
                                .addNamespace("urn:x-cast:com.soundcloud.cast.sender")
                                .enableLockScreen()
                                .enableDebug()
                                .enableNotification()
                                .enableWifiReconnection()
                                .build()
                );
    }

    @Provides
    @Singleton
    public CastPlayer provideCastPlayer(FeatureFlags featureFlags,
                                        Lazy<DefaultCastOperations> castOperations,
                                        Lazy<LegacyCastOperations> legacyCastOperations,
                                        Lazy<VideoCastManager> videoCastManager,
                                        ProgressReporter progressReporter,
                                        PlayQueueManager playQueueManager,
                                        EventBus eventBus,
                                        PlayStatePublisher playStatePublisher,
                                        CurrentDateProvider dateProvider,
                                        CastProtocol castProtocol,
                                        PlaySessionStateProvider playSessionStateProvider,
                                        Provider<ExpandPlayerSubscriber> expandPlayerSubscriber) {
        if (featureFlags.isDisabled(Flag.CAST_V3)) {
            return new LegacyCastPlayer(legacyCastOperations.get(), videoCastManager.get(), progressReporter,
                                        playQueueManager, eventBus, playStatePublisher, dateProvider);
        } else {
            return new DefaultCastPlayer(castOperations.get(), progressReporter, playQueueManager, eventBus,
                                         playStatePublisher, dateProvider, castProtocol, playSessionStateProvider,
                                         expandPlayerSubscriber);
        }
    }

}
