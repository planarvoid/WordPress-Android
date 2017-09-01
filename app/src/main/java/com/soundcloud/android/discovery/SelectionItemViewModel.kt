package com.soundcloud.android.discovery

import com.soundcloud.android.events.EventContextMetadata
import com.soundcloud.android.events.UIEvent
import com.soundcloud.android.image.ImageStyle
import com.soundcloud.android.model.Urn
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.java.optional.Optional

data class SelectionItemViewModel(val urn: Optional<Urn> = Optional.absent(),
                                  val selectionUrn: Urn,
                                  val artworkUrlTemplate: Optional<String> = Optional.absent(),
                                  val artworkStyle: Optional<ImageStyle> = Optional.absent(),
                                  val count: Optional<Int> = Optional.absent(),
                                  val shortTitle: Optional<String> = Optional.absent(),
                                  val shortSubtitle: Optional<String> = Optional.absent(),
                                  val appLink: Optional<String> = Optional.absent(),
                                  val webLink: Optional<String> = Optional.absent(),
                                  val trackingInfo: Optional<TrackingInfo> = Optional.absent()) {
    constructor(selectionItem: SelectionItem, trackingEvent: Optional<TrackingInfo>) :
            this(selectionItem.urn,
                 selectionItem.selectionUrn,
                 selectionItem.artworkUrlTemplate,
                 selectionItem.artworkStyle,
                 selectionItem.count,
                 selectionItem.shortTitle,
                 selectionItem.shortSubtitle,
                 selectionItem.appLink,
                 selectionItem.webLink,
                 trackingEvent)

    fun link() = appLink.or(webLink)

    @OpenForTesting
    data class TrackingInfo(val urn: Optional<Urn>, val eventContextMetadata: EventContextMetadata) {
        fun toUIEvent() = UIEvent.fromDiscoveryCard(urn, eventContextMetadata)
    }
}
