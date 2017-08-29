package com.soundcloud.android.ads

import com.soundcloud.android.model.Urn
import com.soundcloud.java.optional.Optional
import java.util.*

data class AdRequestData(
        val monetizableTrackUrn: Urn? = null,
        val kruxSegments: String? = null,
        val requestId: String = UUID.randomUUID().toString()
) {
    companion object {
        @JvmStatic
        fun forPlayerAd(monetizableTrackUrn: Urn, kruxSegments: Optional<String> = Optional.absent()): AdRequestData {
            return AdRequestData(monetizableTrackUrn, kruxSegments.orNull())
        }

        @JvmStatic
        fun forPageAds(kruxSegments: Optional<String> = Optional.absent()): AdRequestData {
            return AdRequestData(kruxSegments = kruxSegments.orNull())
        }
    }
}
