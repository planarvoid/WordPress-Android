package com.soundcloud.android.discovery

import com.soundcloud.android.model.Urn

sealed class DiscoveryCardViewModel {
    abstract val parentQueryUrn: Urn?

    data class MultipleContentSelectionCard(override val parentQueryUrn: Urn?,
                                            val selectionUrn: Urn,
                                            val queryUrn: Urn?,
                                            val style: String?,
                                            val title: String?,
                                            val description: String?,
                                            val selectionItems: List<SelectionItemViewModel>) :
            DiscoveryCardViewModel() {
        constructor(card: DiscoveryCard.MultipleContentSelectionCard, selectionItems: List<SelectionItemViewModel>) : this(
                parentQueryUrn = card.parentQueryUrn,
                selectionUrn = card.selectionUrn,
                queryUrn = card.queryUrn,
                style = card.style,
                title = card.title,
                description = card.description,
                selectionItems = selectionItems
        )
    }

    data class SingleContentSelectionCard(override val parentQueryUrn: Urn?,
                                          val selectionUrn: Urn,
                                          val queryUrn: Urn?,
                                          val style: String?,
                                          val title: String?,
                                          val description: String?,
                                          val selectionItem: SelectionItemViewModel,
                                          val socialProof: String?,
                                          val socialProofAvatarUrlTemplates: List<String> = emptyList()) : DiscoveryCardViewModel() {
        internal constructor(card: DiscoveryCard.SingleContentSelectionCard, selectionItem: SelectionItemViewModel) : this(
                parentQueryUrn = card.parentQueryUrn,
                selectionUrn = card.selectionUrn,
                queryUrn = card.queryUrn,
                style = card.style,
                title = card.title,
                description = card.description,
                socialProof = card.socialProof,
                socialProofAvatarUrlTemplates = card.socialProofAvatarUrlTemplates,
                selectionItem = selectionItem
        )
    }

    data class EmptyCard(val throwable: Throwable? = null) : DiscoveryCardViewModel() {
        override val parentQueryUrn: Urn? = null
    }

    object SearchCard : DiscoveryCardViewModel() {
        override val parentQueryUrn: Urn? = null
    }
}
