package com.soundcloud.android.cast;

import com.google.android.gms.cast.framework.CastContext;
import com.google.android.libraries.cast.companionlibrary.cast.CastConfiguration;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.soundcloud.android.cast.legacy.LegacyCastConnectionHelper;
import com.soundcloud.android.cast.legacy.LegacyCastOperations;
import com.soundcloud.android.cast.legacy.LegacyCastPlayer;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionStateProvider;
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

import javax.inject.Singleton;

@Module
public class CastModule {

    private static final int REQUIRED_GOOGLE_PLAY_SERVICE_VERSION = 9256000;

    @Provides
    @Singleton
    public CastContextWrapper provideCastContext(GooglePlayServicesWrapper googlePlayServicesWrapper,
                                                 FeatureFlags flags,
                                                 Context context) {
        try {
            if (isCastV3Enabled(flags, googlePlayServicesWrapper, context)) {
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
                                                            Lazy<VideoCastManager> videoCastManage,
                                                            GooglePlayServicesWrapper gpsWrapper,
                                                            FeatureFlags featureFlags,
                                                            Lazy<DefaultCastButtonInstaller> castButtonInstaller) {
        // The dalvik switch is a horrible hack to prevent instantiation of the real cast manager in unit tests as it crashes on robolectric.
        // This is temporary, until we play https://soundcloud.atlassian.net/browse/MC-213
        if ("Dalvik".equals(System.getProperty("java.vm.name"))) {
            if (featureFlags.isDisabled(Flag.CAST_V3)) {
                return new LegacyCastConnectionHelper(videoCastManage.get());
            } else if (gpsWrapper.isPlayServiceAvailable(context)) {
                return new DefaultCastConnectionHelper(castContext.get(), castButtonInstaller.get());
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
                                        Lazy<LegacyCastOperations> legacyCastOperations,
                                        Lazy<VideoCastManager> videoCastManager,
                                        Lazy<ProgressReporter> progressReporter,
                                        PlayQueueManager playQueueManager,
                                        EventBus eventBus,
                                        CastPlayStatePublisher playStatePublisher,
                                        CurrentDateProvider dateProvider,
                                        CastProtocol castProtocol,
                                        PlaySessionStateProvider playSessionStateProvider,
                                        CastQueueController castQueueController,
                                        CastPlayStateReporter castPlayStateReporter,
                                        CastQueueSlicer castQueueSlicer) {
        if (featureFlags.isDisabled(Flag.CAST_V3)) {
            return new LegacyCastPlayer(legacyCastOperations.get(), videoCastManager.get(), progressReporter.get(),
                                        playQueueManager, eventBus, playStatePublisher, dateProvider);
        } else {
            return new DefaultCastPlayer(playQueueManager, eventBus,
                                         castProtocol, playSessionStateProvider,
                                         castQueueController, castPlayStateReporter, castQueueSlicer);
        }
    }

    private boolean isCastV3Enabled(FeatureFlags featureFlags, GooglePlayServicesWrapper googlePlayServicesWrapper, Context context) {
        return featureFlags.isEnabled(Flag.CAST_V3) && googlePlayServicesWrapper.isPlayServiceAvailable(context, REQUIRED_GOOGLE_PLAY_SERVICE_VERSION);
    }

}
