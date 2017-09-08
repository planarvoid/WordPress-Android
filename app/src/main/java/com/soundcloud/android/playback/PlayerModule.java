package com.soundcloud.android.playback;

import com.soundcloud.android.crypto.CryptoOperations;
import com.soundcloud.android.playback.flipper.FlipperCache;
import com.soundcloud.android.playback.flipper.FlipperConfiguration;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.TelphonyBasedCountryProvider;
import dagger.Module;
import dagger.Provides;

import android.content.Context;
import android.support.annotation.Nullable;

import javax.inject.Named;
import java.io.File;
import java.nio.charset.Charset;

@Module
public class PlayerModule {
    private static final String FLIPPER_CACHE_PREFERENCE_KEY = "flipper_cache";
    private static final String FLIPPER_CACHE_KEY = "FlipperCacheKey";

    @Named(FLIPPER_CACHE_KEY)
    @Provides
    public String provideFlipperCacheKey(CryptoOperations cryptoOperations) {
        return new String(cryptoOperations.getKeyOrGenerateAndStore(FLIPPER_CACHE_PREFERENCE_KEY), Charset.forName("UTF-8"));
    }

    @Provides
    public FlipperConfiguration provideFlipperConfiguration(Context context,
                                                            TelphonyBasedCountryProvider countryProvider,
                                                            IOUtils ioUtils,
                                                            @Nullable @Named(FLIPPER_CACHE_KEY) String cacheKey,
                                                            @Nullable @Named(StorageModule.STREAM_CACHE_DIRECTORY_FLIPPER) File streamCacheDirectory,
                                                            FeatureFlags featureFlags) {
        FlipperCache cache = new StreamCacheConfig.FlipperConfig(context, countryProvider, ioUtils, cacheKey, streamCacheDirectory);
        return new FlipperConfiguration(cache, featureFlags.isEnabled(Flag.ENCRYPTED_HLS));
    }
}
