package com.soundcloud.android.discovery

import com.soundcloud.android.view.ViewError
import com.soundcloud.java.optional.Optional

data class DiscoveryResult(val cards: List<DiscoveryCard> = emptyList(), val syncError: Optional<ViewError> = Optional.absent())
