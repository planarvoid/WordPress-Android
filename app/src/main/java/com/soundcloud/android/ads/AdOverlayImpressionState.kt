package com.soundcloud.android.ads

import com.soundcloud.android.model.Urn
import com.soundcloud.android.playback.TrackSourceInfo

data class AdOverlayImpressionState(
        val isAdOverlayVisible: Boolean,
        val isAppInForeground: Boolean,
        val isPlayerExpanding: Boolean,
        val currentPlayingUrn: Urn?,
        val adData: VisualAdData?,
        val trackSourceInfo: TrackSourceInfo?
)

data class VisualAdImpressionState(
        val adData: AdData,
        val isAppInForeground: Boolean,
        val isPlayerExpanding: Boolean
)
