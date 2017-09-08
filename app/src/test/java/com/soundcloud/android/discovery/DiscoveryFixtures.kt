package com.soundcloud.android.discovery

import com.soundcloud.android.model.Urn
import com.soundcloud.java.collections.Lists

internal object DiscoveryFixtures {
    private val singleSelectionUrn = Urn.forSystemPlaylist("upload")
    private val multiSelectionUrn = Urn.forSystemPlaylist("chilling")
    private val singleItemUrn = Urn.forPlaylist(123)
    private val multiItemUrn = Urn.forPlaylist(124)
    val queryUrn = Urn("soundcloud:discovery:123")
    val parentQueryUrn = Urn("soundcloud:discovery:123")
    val singleAppLink = "soundcloud://playlists/123"
    val multiAppLink = "soundcloud://playlists/124"
    val singleWebLink = "www://soundcloud.com/playlists/123"
    val multiWebLink = "www://soundcloud.com/playlists/124"
    val singleSelectionItem = SelectionItem(singleItemUrn,
                                            singleSelectionUrn,
                                            null, null, null, null, null,
                                            singleAppLink,
                                            singleWebLink)
    val multiSelectionItem = SelectionItem(multiItemUrn,
                                           multiSelectionUrn, null, null, null, null, null,
                                           multiAppLink,
                                           multiWebLink)
    val singleContentSelectionCard = DiscoveryCard.SingleContentSelectionCard(parentQueryUrn,
                                                                              singleSelectionUrn, null, null, null,
                                                                              queryUrn,
                                                                              singleSelectionItem, null, null,
                                                                              Lists.newArrayList())
    val multipleContentSelectionCard = DiscoveryCard.MultipleContentSelectionCard(parentQueryUrn,
                                                                                  multiSelectionUrn,
                                                                                  queryUrn, null, null, null, null,
                                                                                  Lists.newArrayList(multiSelectionItem))

    fun singleContentSelectionCardViewModelWithSelectionItem(selectionItem: SelectionItemViewModel): DiscoveryCardViewModel.SingleContentSelectionCard {
        return DiscoveryCardViewModel.SingleContentSelectionCard(singleContentSelectionCard, selectionItem)
    }

    fun singleContentSelectionCardViewModel(): DiscoveryCardViewModel.SingleContentSelectionCard {
        return DiscoveryCardViewModel.SingleContentSelectionCard(singleContentSelectionCard, SelectionItemViewModel(singleContentSelectionCard.selectionItem, null))
    }

    fun multiContentSelectionCardViewModel(): DiscoveryCardViewModel.MultipleContentSelectionCard {
        return DiscoveryCardViewModel.MultipleContentSelectionCard(multipleContentSelectionCard,
                                                                   multipleContentSelectionCard.selectionItems.map { item -> SelectionItemViewModel(item, null) })
    }

    fun singleSelectionItemViewModel(trackingInfo: SelectionItemTrackingInfo): SelectionItemViewModel {
        return SelectionItemViewModel(singleSelectionItem, trackingInfo)
    }

    fun multiSelectionItemViewModel(trackingInfo: SelectionItemTrackingInfo): SelectionItemViewModel {
        return SelectionItemViewModel(multiSelectionItem, trackingInfo)
    }
}
