package com.soundcloud.android.playback.flipper

import com.soundcloud.java.strings.Strings

data class FlipperException(override val message: String, private val line: Int, private val sourceFile: String) : Exception() {
    override fun getStackTrace() = arrayOf(StackTraceElement(message, Strings.EMPTY, sourceFile, line))
}
