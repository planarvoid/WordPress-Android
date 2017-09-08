package com.soundcloud.android.discovery;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

final class DiscoveryCardMapper {

    private DiscoveryCardMapper() {
        // not used
    }

    static DiscoveryCard map(ApiDiscoveryCard apiDiscoveryCard, Optional<Urn> pageQueryUrn) {
        if (apiDiscoveryCard.multipleContentSelectionCard().isPresent()) {
            final ApiMultipleContentSelectionCard apiMultipleContentSelectionCard = apiDiscoveryCard.multipleContentSelectionCard().get();
            final Urn selectionUrn = apiMultipleContentSelectionCard.selectionUrn();
            return new DiscoveryCard.MultipleContentSelectionCard(apiMultipleContentSelectionCard.selectionItems().getQueryUrn().orNull(),
                                                                  selectionUrn,
                                                                  pageQueryUrn.orNull(),
                                                                  apiMultipleContentSelectionCard.style().orNull(),
                                                                  apiMultipleContentSelectionCard.title().orNull(),
                                                                  apiMultipleContentSelectionCard.description().orNull(),
                                                                  apiMultipleContentSelectionCard.trackingFeatureName().orNull(),
                                                                  apiMultipleContentSelectionCard.selectionItems()
                                                                                                 .transform(item -> DiscoveryCardMapper.map(selectionUrn, item))
                                                                                                 .getCollection());
        } else if (apiDiscoveryCard.singleContentSelectionCard().isPresent()) {
            final ApiSingleContentSelectionCard apiSingleContentSelectionCard = apiDiscoveryCard.singleContentSelectionCard().get();
            final Urn selectionUrn = apiSingleContentSelectionCard.selectionUrn();
            return new DiscoveryCard.SingleContentSelectionCard(apiSingleContentSelectionCard.queryUrn().orNull(),
                                                                selectionUrn,
                                                                apiSingleContentSelectionCard.style().orNull(),
                                                                apiSingleContentSelectionCard.title().orNull(),
                                                                apiSingleContentSelectionCard.description().orNull(),
                                                                pageQueryUrn.orNull(),
                                                                map(selectionUrn, apiSingleContentSelectionCard.selectionItem()),
                                                                apiSingleContentSelectionCard.trackingFeatureName().orNull(),
                                                                apiSingleContentSelectionCard.socialProof().orNull(),
                                                                apiSingleContentSelectionCard.socialProofAvatarUrlTemplates());
        } else {
            throw new IllegalStateException("Unexpected card type");
        }
    }

    private static SelectionItem map(Urn selectionUrn, ApiSelectionItem apiSelectionItem) {
        return new SelectionItem(apiSelectionItem.urn().orNull(),
                                 selectionUrn,
                                 apiSelectionItem.artworkUrlTemplate().orNull(),
                                 apiSelectionItem.artworkStyle().orNull(),
                                 apiSelectionItem.count().orNull(),
                                 apiSelectionItem.shortTitle().orNull(),
                                 apiSelectionItem.shortSubtitle().orNull(),
                                 apiSelectionItem.appLink().orNull(),
                                 apiSelectionItem.webLink().orNull());
    }
}
