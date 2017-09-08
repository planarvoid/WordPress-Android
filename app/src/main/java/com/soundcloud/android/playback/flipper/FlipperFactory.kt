package com.soundcloud.android.playback.flipper

import com.soundcloud.flippernative.Decoder
import com.soundcloud.flippernative.api.Player
import com.soundcloud.flippernative.api.PlayerConfiguration
import com.soundcloud.flippernative.api.PlayerListener
import com.soundcloud.java.strings.Strings
import javax.inject.Inject

internal class FlipperFactory
@Inject
constructor(private val flipperConfiguration: FlipperConfiguration) {

    fun create(listener: PlayerListener): Player = with(flipperConfiguration) {
        Player.setLogLevel(Player.LogLevel.Debug)
        val player = Player(PlayerConfiguration(
                cache.directory()?.absolutePath ?: Strings.EMPTY,
                cache.key(),
                cache.size(),
                cache.minFreeSpaceAvailablePercentage(),
                PROGRESS_INTERVAL_MS,
                forceEncryptedHls,
                cache.logFilePath()
        ), listener)
        player.setMediaCodecDelegate(DECODER)
        return player
    }

    companion object {

        private val PROGRESS_INTERVAL_MS = 500L

        // TODO : Flipper should implement it itself
        // We have to keep the reference to avoid the GC to collect it. Otherwise,
        // it crashes the app.
        private val DECODER = Decoder()
    }
}
