package com.soundcloud.android.playback

import com.soundcloud.android.model.Urn

class MissingTrackException(val trackUrn: Urn) : Throwable(trackUrn.toString())
