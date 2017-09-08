package com.soundcloud.android.discovery

import com.soundcloud.android.model.Urn
import com.soundcloud.java.optional.Optional

sealed class DiscoveryCard {

    abstract val parentQueryUrn: Urn?

    internal open fun hasSelectionUrn(selectionUrn: Urn): Boolean = false

    data class MultipleContentSelectionCard(override val parentQueryUrn: Urn? = null,
                                            val selectionUrn: Urn,
                                            val queryUrn: Urn? = null,
                                            val style: String? = null,
                                            val title: String? = null,
                                            val description: String? = null,
                                            val trackingFeatureName: String? = null,
                                            val selectionItems: List<SelectionItem>) : DiscoveryCard() {

        override fun hasSelectionUrn(selectionUrn: Urn): Boolean = this.selectionUrn == selectionUrn
    }

    data class SingleContentSelectionCard(override val parentQueryUrn: Urn? = null,
                                          val selectionUrn: Urn,
                                          val style: String? = null,
                                          val title: String? = null,
                                          val description: String? = null,
                                          val queryUrn: Urn? = null,
                                          val selectionItem: SelectionItem,
                                          val trackingFeatureName: String? = null,
                                          val socialProof: String? = null,
                                          val socialProofAvatarUrlTemplates: List<String>) : DiscoveryCard() {

        override fun hasSelectionUrn(selectionUrn: Urn): Boolean = this.selectionUrn == selectionUrn
    }

    data class EmptyCard(val throwable: Throwable? = null, override val parentQueryUrn: Urn? = null) : DiscoveryCard() {
        constructor(optionalThrowable: Optional<Throwable>) : this(optionalThrowable.orNull())
    }
}
