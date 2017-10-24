package com.soundcloud.android.playback.flipper

import com.soundcloud.android.utils.OpenForTesting

@OpenForTesting
data class FlipperConfiguration(val cache: FlipperCache, val forceEncryptedHls: Boolean, val shouldCrashOnHang: Boolean)
