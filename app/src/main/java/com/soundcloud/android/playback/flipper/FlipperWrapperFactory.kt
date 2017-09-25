package com.soundcloud.android.playback.flipper

import com.soundcloud.android.utils.OpenForTesting
import javax.inject.Inject
import javax.inject.Singleton

@OpenForTesting
@Singleton
internal class FlipperWrapperFactory
@Inject
constructor(private val flipperFactory: FlipperFactory) {
    fun create(flipperCallbacks: FlipperCallbacks) = FlipperWrapper(flipperCallbacks, flipperFactory)
}
