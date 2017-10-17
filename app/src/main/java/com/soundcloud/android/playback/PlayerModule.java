package com.soundcloud.android.playback;

import com.soundcloud.android.crypto.CryptoOperations;
import com.soundcloud.android.playback.flipper.FlipperCache;
import com.soundcloud.android.playback.flipper.FlipperConfiguration;
import com.soundcloud.android.playback.flipper.FlipperPerformanceReporter;
import com.soundcloud.android.playback.skippy.SkippyPerformanceReporter;
import com.soundcloud.android.playback.skippy.SkippyCache;
import com.soundcloud.android.playback.skippy.SkippyConfiguration;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.TelphonyBasedCountryProvider;
import com.soundcloud.rx.eventbus.EventBusV2;
import dagger.Module;
import dagger.Provides;

import android.content.Context;
import android.support.annotation.Nullable;

import javax.inject.Named;
import java.io.File;
import java.nio.charset.Charset;

@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod") // abstract to force @Provides methods to be static
@Module
public abstract class PlayerModule {
    private static final String SKIPPY_CACHE_PREFERENCE_KEY = "skippy_cache";
    private static final String SKIPPY_CACHE_KEY = "SkippyCacheKey";

    private static final String FLIPPER_CACHE_PREFERENCE_KEY = "flipper_cache";
    private static final String FLIPPER_CACHE_KEY = "FlipperCacheKey";

    @Named(SKIPPY_CACHE_KEY)
    @Provides
    static byte[] provideSkippyCacheKey(CryptoOperations cryptoOperations) {
        return cryptoOperations.getKeyOrGenerateAndStore(SKIPPY_CACHE_PREFERENCE_KEY);
    }

    @Provides
    static SkippyConfiguration provideSkippyConfiguration(Context context,
                                                          TelphonyBasedCountryProvider countryProvider,
                                                          IOUtils ioUtils,
                                                          @Nullable @Named(SKIPPY_CACHE_KEY) byte[] cacheKey,
                                                          @Nullable @Named(StorageModule.STREAM_CACHE_DIRECTORY_SKIPPY) File streamCacheDirectory,
                                                          ApplicationProperties applicationProperties) {
        SkippyCache cache = new StreamCacheConfig.SkippyConfig(context, countryProvider, ioUtils, cacheKey, streamCacheDirectory);
        return new SkippyConfiguration(context, cache, !applicationProperties.isReleaseBuild());
    }

    @Named(FLIPPER_CACHE_KEY)
    @Provides
    static String provideFlipperCacheKey(CryptoOperations cryptoOperations) {
        return new String(cryptoOperations.getKeyOrGenerateAndStore(FLIPPER_CACHE_PREFERENCE_KEY), Charset.forName("UTF-8"));
    }

    @Provides
    static FlipperConfiguration provideFlipperConfiguration(Context context,
                                                            TelphonyBasedCountryProvider countryProvider,
                                                            IOUtils ioUtils,
                                                            @Nullable @Named(FLIPPER_CACHE_KEY) String cacheKey,
                                                            @Nullable @Named(StorageModule.STREAM_CACHE_DIRECTORY_FLIPPER) File streamCacheDirectory,
                                                            FeatureFlags featureFlags) {
        FlipperCache cache = new StreamCacheConfig.FlipperConfig(context, countryProvider, ioUtils, cacheKey, streamCacheDirectory);
        return new FlipperConfiguration(cache, featureFlags.isEnabled(Flag.ENCRYPTED_HLS));
    }

    @Provides
    static SkippyPerformanceReporter provideSkippyPerformanceReporter(EventBusV2 eventBus) {
        return new SkippyPerformanceReporter(eventBus);
    }

    @Provides
    static FlipperPerformanceReporter provideFlipperPerformanceReporter(EventBusV2 eventBus) {
        return new FlipperPerformanceReporter(eventBus);
    }
}
