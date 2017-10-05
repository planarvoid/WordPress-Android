package com.soundcloud.android.playback

import java.io.File

interface PlayerCache<out Key> {
    fun key(): Key
    fun directory(): File?
    fun size(): Long
    fun minFreeSpaceAvailablePercentage(): Byte
    fun remainingSpace(): Long
}
