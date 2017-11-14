package com.soundcloud.android.search

import com.soundcloud.android.model.Urn
import com.soundcloud.android.presentation.ListItem
import com.soundcloud.android.tracks.TrackItem
import com.soundcloud.java.optional.Optional

data class SearchTrackItem(val trackItem: TrackItem,
                           val queryUrn: Optional<Urn>,
                           override val urn: Urn = trackItem.urn,
                           override val imageUrlTemplate: Optional<String> = trackItem.imageUrlTemplate) : ListItem
