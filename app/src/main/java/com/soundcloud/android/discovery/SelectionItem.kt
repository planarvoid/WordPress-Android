package com.soundcloud.android.discovery

import com.soundcloud.android.image.ImageStyle
import com.soundcloud.android.model.Urn
import com.soundcloud.java.optional.Optional

data class SelectionItem(val urn: Optional<Urn> = Optional.absent(),
                         val selectionUrn: Urn,
                         val artworkUrlTemplate: Optional<String> = Optional.absent(),
                         val artworkStyle: Optional<ImageStyle> = Optional.absent(),
                         val count: Optional<Int> = Optional.absent(),
                         val shortTitle: Optional<String> = Optional.absent(),
                         val shortSubtitle: Optional<String> = Optional.absent(),
                         val appLink: Optional<String> = Optional.absent(),
                         val webLink: Optional<String> = Optional.absent())
