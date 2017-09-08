package com.soundcloud.android.discovery

import com.soundcloud.android.image.ImageStyle
import com.soundcloud.android.model.Urn

data class SelectionItemViewModel(val urn: Urn? = null,
                                  val selectionUrn: Urn,
                                  val artworkUrlTemplate: String? = null,
                                  val artworkStyle: ImageStyle? = null,
                                  val count: Int? = null,
                                  val shortTitle: String? = null,
                                  val shortSubtitle: String? = null,
                                  val appLink: String? = null,
                                  val webLink: String? = null,
                                  val trackingInfo: SelectionItemTrackingInfo? = null) {
    constructor(selectionItem: SelectionItem, trackingInfo: SelectionItemTrackingInfo?) :
            this(selectionItem.urn,
                 selectionItem.selectionUrn,
                 selectionItem.artworkUrlTemplate,
                 selectionItem.artworkStyle,
                 selectionItem.count,
                 selectionItem.shortTitle,
                 selectionItem.shortSubtitle,
                 selectionItem.appLink,
                 selectionItem.webLink,
                 trackingInfo)

    fun link() = appLink ?: webLink

}
