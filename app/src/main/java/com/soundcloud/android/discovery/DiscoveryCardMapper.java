package com.soundcloud.android.discovery;

import com.soundcloud.java.collections.Lists;

class DiscoveryCardMapper {

    static DiscoveryCard map(ApiDiscoveryCard apiDiscoveryCard) {
        if (apiDiscoveryCard.multipleContentSelectionCard().isPresent()) {
            final ApiMultipleContentSelectionCard apiMultipleContentSelectionCard = apiDiscoveryCard.multipleContentSelectionCard().get();
            return DiscoveryCard.MultipleContentSelectionCard.create(apiMultipleContentSelectionCard.selectionUrn(),
                                                                     apiMultipleContentSelectionCard.style(),
                                                                     apiMultipleContentSelectionCard.title(),
                                                                     apiMultipleContentSelectionCard.description(),
                                                                     apiMultipleContentSelectionCard.selectionItems()
                                                                                                    .transform(apiSelectionPlaylists ->
                                                                                                                    Lists.transform(apiSelectionPlaylists.getCollection(), DiscoveryCardMapper::map)));
        } else if (apiDiscoveryCard.singleContentSelectionCard().isPresent()) {
            final ApiSingleContentSelectionCard apiSingleContentSelectionCard = apiDiscoveryCard.singleContentSelectionCard().get();
            return DiscoveryCard.SingleContentSelectionCard.create(apiSingleContentSelectionCard.selectionUrn(),
                                                                   apiSingleContentSelectionCard.queryUrn(),
                                                                   apiSingleContentSelectionCard.style(),
                                                                   apiSingleContentSelectionCard.title(),
                                                                   apiSingleContentSelectionCard.description(),
                                                                   map(apiSingleContentSelectionCard.selectionItem()),
                                                                   apiSingleContentSelectionCard.socialProof(),
                                                                   apiSingleContentSelectionCard.socialProofAvatarUrlTemplates());
        } else {
            throw new IllegalStateException("Unexpected card type");
        }
    }

    private static SelectionItem map(ApiSelectionItem apiSelectionItem) {
        return SelectionItem.create(apiSelectionItem.urn(),
                                    apiSelectionItem.artworkUrlTemplate(),
                                    apiSelectionItem.count(),
                                    apiSelectionItem.shortTitle(),
                                    apiSelectionItem.shortSubtitle());
    }
}
