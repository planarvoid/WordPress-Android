package com.soundcloud.android.discovery;

import com.soundcloud.android.model.Urn;

final class DiscoveryCardMapper {

    private DiscoveryCardMapper() {
        // not used
    }

    static DiscoveryCard map(ApiDiscoveryCard apiDiscoveryCard) {
        if (apiDiscoveryCard.multipleContentSelectionCard().isPresent()) {
            final ApiMultipleContentSelectionCard apiMultipleContentSelectionCard = apiDiscoveryCard.multipleContentSelectionCard().get();
            final Urn selectionUrn = apiMultipleContentSelectionCard.selectionUrn();
            return DiscoveryCard.MultipleContentSelectionCard.create(selectionUrn,
                                                                     apiMultipleContentSelectionCard.selectionItems().getQueryUrn(),
                                                                     apiMultipleContentSelectionCard.style(),
                                                                     apiMultipleContentSelectionCard.title(),
                                                                     apiMultipleContentSelectionCard.description(),
                                                                     apiMultipleContentSelectionCard.selectionItems()
                                                                                                    .transform(item -> DiscoveryCardMapper.map(selectionUrn, item))
                                                                                                    .getCollection());
        } else if (apiDiscoveryCard.singleContentSelectionCard().isPresent()) {
            final ApiSingleContentSelectionCard apiSingleContentSelectionCard = apiDiscoveryCard.singleContentSelectionCard().get();
            final Urn selectionUrn = apiSingleContentSelectionCard.selectionUrn();
            return DiscoveryCard.SingleContentSelectionCard.create(selectionUrn,
                                                                   apiSingleContentSelectionCard.queryUrn(),
                                                                   apiSingleContentSelectionCard.style(),
                                                                   apiSingleContentSelectionCard.title(),
                                                                   apiSingleContentSelectionCard.description(),
                                                                   map(selectionUrn, apiSingleContentSelectionCard.selectionItem()),
                                                                   apiSingleContentSelectionCard.socialProof(),
                                                                   apiSingleContentSelectionCard.socialProofAvatarUrlTemplates());
        } else {
            throw new IllegalStateException("Unexpected card type");
        }
    }

    private static SelectionItem map(Urn selectionUrn, ApiSelectionItem apiSelectionItem) {
        return SelectionItem.create(apiSelectionItem.urn(),
                                    selectionUrn,
                                    apiSelectionItem.artworkUrlTemplate(),
                                    apiSelectionItem.artworkStyle(),
                                    apiSelectionItem.count(),
                                    apiSelectionItem.shortTitle(),
                                    apiSelectionItem.shortSubtitle(),
                                    apiSelectionItem.appLink(),
                                    apiSelectionItem.webLink());
    }
}
