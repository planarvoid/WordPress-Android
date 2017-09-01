package com.soundcloud.android.discovery

import com.soundcloud.android.model.Urn
import com.soundcloud.java.optional.Optional

sealed class DiscoveryCardViewModel(val kind: DiscoveryCard.Kind) {
    abstract val parentQueryUrn: Optional<Urn>

    internal data class MultipleContentSelectionCard(override val parentQueryUrn: Optional<Urn>,
                                                     val selectionUrn: Urn,
                                                     val queryUrn: Optional<Urn>,
                                                     val style: Optional<String>,
                                                     val title: Optional<String>,
                                                     val description: Optional<String>,
                                                     val selectionItems: List<SelectionItemViewModel>) :
            DiscoveryCardViewModel(DiscoveryCard.Kind.MULTIPLE_CONTENT_SELECTION_CARD) {
        constructor(card: DiscoveryCard.MultipleContentSelectionCard, selectionItems: List<SelectionItemViewModel>) : this(
                parentQueryUrn = card.parentQueryUrn(),
                selectionUrn = card.selectionUrn(),
                queryUrn = card.queryUrn(),
                style = card.style(),
                title = card.title(),
                description = card.description(),
                selectionItems = selectionItems
        )
    }

    internal data class SingleContentSelectionCard(override val parentQueryUrn: Optional<Urn>,
                                                   val selectionUrn: Urn,
                                                   val queryUrn: Optional<Urn> = Optional.absent(),
                                                   val style: Optional<String> = Optional.absent(),
                                                   val title: Optional<String> = Optional.absent(),
                                                   val description: Optional<String> = Optional.absent(),
                                                   val selectionItem: SelectionItemViewModel,
                                                   val socialProof: Optional<String> = Optional.absent(),
                                                   val socialProofAvatarUrlTemplates: List<String> = emptyList()) : DiscoveryCardViewModel(DiscoveryCard.Kind.SINGLE_CONTENT_SELECTION_CARD) {
        constructor(card: DiscoveryCard.SingleContentSelectionCard, selectionItem: SelectionItemViewModel) : this(
                parentQueryUrn = card.parentQueryUrn(),
                selectionUrn = card.selectionUrn(),
                queryUrn = card.queryUrn(),
                style = card.style(),
                title = card.title(),
                description = card.description(),
                socialProof = card.socialProof(),
                socialProofAvatarUrlTemplates = card.socialProofAvatarUrlTemplates(),
                selectionItem = selectionItem
        )
    }

    data class EmptyCard(val throwable: Optional<Throwable> = Optional.absent()) : DiscoveryCardViewModel(DiscoveryCard.Kind.EMPTY_CARD) {
        override val parentQueryUrn: Optional<Urn> = Optional.absent<Urn>()
    }

    object SearchCard : DiscoveryCardViewModel(DiscoveryCard.Kind.SEARCH_ITEM) {
        override val parentQueryUrn: Optional<Urn> = Optional.absent<Urn>()
    }
}
