package com.soundcloud.android.discovery

import com.soundcloud.android.image.ImageStyle
import com.soundcloud.android.model.Urn

data class SelectionItem(val urn: Urn? = null,
                         val selectionUrn: Urn,
                         val artworkUrlTemplate: String? = null,
                         val artworkStyle: ImageStyle? = null,
                         val count: Int? = null,
                         val shortTitle: String? = null,
                         val shortSubtitle: String? = null,
                         val appLink: String? = null,
                         val webLink: String? = null)
