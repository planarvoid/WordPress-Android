package com.soundcloud.android.playback.flipper

import com.soundcloud.flippernative.api.StreamingProtocol
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlipperErrorTest {
    @Test
    fun networkErrorsAreBasedOnCategoryString() {
        assertFalse(FlipperError("not-a-network-error", "sourceFile", 123, "message", StreamingProtocol.Hls, "cdn", "format", 128).isNetworkError())
        assertTrue(FlipperError("hls_stream-network", "sourceFile", 123, "message", StreamingProtocol.Hls, "cdn", "format", 128).isNetworkError())
    }
}
