package com.soundcloud.android.playback.flipper;

import com.soundcloud.android.crypto.CryptoOperations;
import com.soundcloud.android.playback.StreamCacheConfig;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.flippernative.Decoder;
import com.soundcloud.flippernative.api.Player;
import com.soundcloud.flippernative.api.PlayerConfiguration;
import com.soundcloud.flippernative.api.PlayerListener;
import com.soundcloud.java.strings.Strings;

import android.content.Context;

import javax.inject.Inject;
import java.io.File;
import java.io.UnsupportedEncodingException;

class FlipperFactory {

    private static final String KEY_PREFERENCE_NAME = "flipper_cache";
    private static final int PROGRESS_INTERVAL_MS = 500;

    private final Context context;
    private final CryptoOperations cryptoOperations;
    private final StreamCacheConfig.FlipperConfig cacheConfig;

    @Inject
    FlipperFactory(Context context,
                   CryptoOperations cryptoOperations,
                   StreamCacheConfig.FlipperConfig cacheConfig) {
        this.context = context;
        this.cryptoOperations = cryptoOperations;
        this.cacheConfig = cacheConfig;
    }

    public Player create(PlayerListener listener) {
        Player.setLogLevel(com.soundcloud.flippernative.api.Player.LogLevel.Debug);

        final Player player = new Player(getConfiguration(), listener);
        player.setMediaCodecDelegate(new Decoder());
        return player;
    }

    private PlayerConfiguration getConfiguration() {
        return new PlayerConfiguration(
                cacheDirectory(),
                cacheKey(),
                cacheConfig.getStreamCacheSize(),
                cacheConfig.getStreamCacheMinFreeSpaceAvailablePercentage(),
                PROGRESS_INTERVAL_MS,
                cacheConfig.getLogFilePath()
        );
    }

    private String cacheKey() {
        final byte[] key = cryptoOperations.getKeyOrGenerateAndStore(KEY_PREFERENCE_NAME);
        try {
            return new String(key, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // UTF-8 should be supported on every device
            // To be safe we monitor it.
            ErrorUtils.handleSilentException(e);
            return Strings.EMPTY;
        }
    }

    private String cacheDirectory() {
        final File streamCacheDirectory = cacheConfig.getStreamCacheDirectory();
        IOUtils.createCacheDirs(context, streamCacheDirectory);
        return streamCacheDirectory == null ? null : streamCacheDirectory.getAbsolutePath();
    }
}
