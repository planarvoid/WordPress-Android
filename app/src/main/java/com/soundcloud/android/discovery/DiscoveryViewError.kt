package com.soundcloud.android.discovery

import com.soundcloud.android.view.ViewError

data class DiscoveryViewError(val viewError: ViewError, val timestamp: Long = System.currentTimeMillis())
