package com.soundcloud.android.playback.skippy

import com.soundcloud.android.skippy.Skippy
import com.soundcloud.android.skippy.SkippyPreloader
import com.soundcloud.android.utils.OpenForTesting
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@OpenForTesting
internal class SkippyFactory
@Inject
constructor(private val skippyConfiguration: SkippyConfiguration) {

    fun createPreloader(): SkippyPreloader = SkippyPreloader(null)

    @JvmOverloads
    fun create(listener: Skippy.PlayListener? = null) = Skippy(skippyConfiguration.context, listener)

    fun createConfiguration(): Skippy.Configuration = with(skippyConfiguration) {
        return Skippy.Configuration(
                PROGRESS_INTERVAL_MS,
                BUFFER_DURATION_MS,
                cache.size(),
                cache.minFreeSpaceAvailablePercentage().toLong(),
                cache.directory()?.absolutePath,
                cache.key(),
                debuggable,
                Skippy.CacheRestriction.NONE,
                Skippy.SkippyMediaType.OPUS
        )
    }

    companion object {
        private val PROGRESS_INTERVAL_MS = 500
        private val BUFFER_DURATION_MS = TimeUnit.SECONDS.toMillis(90)
    }
}
