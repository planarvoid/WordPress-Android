package com.soundcloud.android.discovery;

final class DiscoveryCardMapper {

    private DiscoveryCardMapper() {
        // not used
    }

    static DiscoveryCard map(ApiDiscoveryCard apiDiscoveryCard) {
        if (apiDiscoveryCard.multipleContentSelectionCard().isPresent()) {
            final ApiMultipleContentSelectionCard apiMultipleContentSelectionCard = apiDiscoveryCard.multipleContentSelectionCard().get();
            return DiscoveryCard.MultipleContentSelectionCard.create(apiMultipleContentSelectionCard.selectionUrn(),
                                                                     apiMultipleContentSelectionCard.style(),
                                                                     apiMultipleContentSelectionCard.title(),
                                                                     apiMultipleContentSelectionCard.description(),
                                                                     apiMultipleContentSelectionCard.selectionItems().transform(DiscoveryCardMapper::map).getCollection());
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
                                    apiSelectionItem.artworkStyle(),
                                    apiSelectionItem.count(),
                                    apiSelectionItem.shortTitle(),
                                    apiSelectionItem.shortSubtitle(),
                                    apiSelectionItem.appLink(),
                                    apiSelectionItem.webLink());
    }
}
