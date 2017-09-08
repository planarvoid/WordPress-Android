package com.soundcloud.android.playback.flipper

import java.io.File

interface FlipperCache {
    fun key(): String
    fun directory(): File?
    fun size(): Long
    fun minFreeSpaceAvailablePercentage(): Byte
    fun logFilePath(): String?
}
