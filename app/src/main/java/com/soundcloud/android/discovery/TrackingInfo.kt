package com.soundcloud.android.discovery

import com.soundcloud.android.events.EventContextMetadata
import com.soundcloud.android.events.Module
import com.soundcloud.android.events.UIEvent
import com.soundcloud.android.main.Screen
import com.soundcloud.android.model.Urn
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.java.collections.Iterables
import com.soundcloud.java.optional.Optional

@OpenForTesting
data class SelectionItemTrackingInfo(val urn: Urn?, val eventContextMetadata: EventContextMetadata) {
    fun toUIEvent() = UIEvent.fromDiscoveryCard(Optional.fromNullable(urn), eventContextMetadata)

    companion object {
        internal val SCREEN = Screen.DISCOVER

        fun create(selectionItem: SelectionItem, parentCard: DiscoveryCard, cardPosition: Int): SelectionItemTrackingInfo? =
                when (parentCard) {
                    is DiscoveryCard.SingleContentSelectionCard -> trackSelectionItemInSingleContentSelectionCard(cardPosition, parentCard)
                    is DiscoveryCard.MultipleContentSelectionCard -> trackSelectionItemInMultipleContentSelectionCard(selectionItem, cardPosition, parentCard)
                    else -> {
                        null
                    }
                }

        private fun trackSelectionItemInSingleContentSelectionCard(selectionPosition: Int, singleContentSelectionCard: DiscoveryCard.SingleContentSelectionCard): SelectionItemTrackingInfo {
            val builder = EventContextMetadata.builder()
            val selectionUrn = singleContentSelectionCard.selectionUrn
            builder.pageName(SCREEN.get())
            builder.source(Optional.fromNullable(singleContentSelectionCard.trackingFeatureName))
            builder.sourceUrn(selectionUrn)
            builder.sourceQueryUrn(Optional.fromNullable(singleContentSelectionCard.queryUrn))
            builder.sourceQueryPosition(0)
            builder.queryPosition(selectionPosition)
            builder.queryUrn(Optional.fromNullable(singleContentSelectionCard.parentQueryUrn))
            return SelectionItemTrackingInfo(singleContentSelectionCard.selectionItem.urn, builder.build())
        }

        private fun trackSelectionItemInMultipleContentSelectionCard(selectionItem: SelectionItem,
                                                                     selectionPosition: Int,
                                                                     multipleContentSelectionCard: DiscoveryCard.MultipleContentSelectionCard): SelectionItemTrackingInfo? {
            val selectionItemUrn = selectionItem.urn
            selectionItemUrn?.let {
                val builder = EventContextMetadata.builder()
                builder.pageName(SCREEN.get())
                builder.source(Optional.fromNullable(multipleContentSelectionCard.trackingFeatureName))
                builder.sourceUrn(multipleContentSelectionCard.selectionUrn)
                builder.sourceQueryUrn(Optional.fromNullable(multipleContentSelectionCard.queryUrn))
                builder.queryPosition(selectionPosition)

                builder.queryUrn(Optional.fromNullable(multipleContentSelectionCard.parentQueryUrn))

                val selectionItems = multipleContentSelectionCard.selectionItems
                val selectionItemOptional = Iterables.tryFind(selectionItems) { item -> item?.urn != null && it == item.urn }
                selectionItemOptional.ifPresent { item ->
                    val itemPosition = selectionItems.indexOf(item)
                    builder.sourceQueryPosition(itemPosition)
                    builder.module(Module.create(multipleContentSelectionCard.selectionUrn.toString(), itemPosition))
                }
                return SelectionItemTrackingInfo(it, builder.build())
            }
            return null
        }
    }
}
