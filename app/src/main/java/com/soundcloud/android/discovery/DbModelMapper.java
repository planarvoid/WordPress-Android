package com.soundcloud.android.discovery;

import com.soundcloud.java.collections.ListMultiMap;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.MultiMap;
import com.soundcloud.java.optional.Optional;

import java.util.Collection;
import java.util.List;

public final class DbModelMapper {
    private DbModelMapper() {
    }

    static MultiMap<Long, DbModel.SelectionItem> toMultiMap(List<DbModel.SelectionItem> list) {
        final MultiMap<Long, DbModel.SelectionItem> multiMap = new ListMultiMap<>();
        for (DbModel.SelectionItem selectionItem : list) {
            multiMap.put(selectionItem.card_id(), selectionItem);
        }
        return multiMap;
    }

    static List<DiscoveryCard> mapDiscoveryCardsWithSelectionItems(List<DbModel.FullDiscoveryCard> fullDiscoveryCards, MultiMap<Long, DbModel.SelectionItem> selectionItems1) {
        return Lists.transform(fullDiscoveryCards, fullDiscoveryCard -> mapDiscoveryCard(fullDiscoveryCard, selectionItems1));
    }

    private static DiscoveryCard mapDiscoveryCard(DbModel.FullDiscoveryCard fullDiscoveryCard, MultiMap<Long, DbModel.SelectionItem> selectionItems1) {
        if (fullDiscoveryCard.single_content_selection_card() != null) {
            final DbModel.SingleContentSelectionCard singleContentSelectionCard = fullDiscoveryCard.single_content_selection_card();
            final Collection<DbModel.SelectionItem> itemForCard = selectionItems1.get(singleContentSelectionCard._id());
            final DbModel.SelectionItem selectionItem = itemForCard.iterator().next();
            return mapSingleContentSelectionCard(singleContentSelectionCard, selectionItem);
        } else if (fullDiscoveryCard.multiple_content_selection_card() != null) {
            final DbModel.MultipleContentSelectionCard singleContentSelectionCard = fullDiscoveryCard.multiple_content_selection_card();
            final Collection<DbModel.SelectionItem> itemsForCard = selectionItems1.get(singleContentSelectionCard._id());
            return mapMultipleContentSelectionCard(singleContentSelectionCard, itemsForCard);
        } else {
            throw new IllegalArgumentException("Unexpected card type");
        }
    }

    private static DiscoveryCard.SingleContentSelectionCard mapSingleContentSelectionCard(DbModel.SingleContentSelectionCard singleContentSelectionCard, DbModel.SelectionItem selectionItem) {
        return DiscoveryCard.SingleContentSelectionCard.create(singleContentSelectionCard.urn(),
                                                               Optional.fromNullable(singleContentSelectionCard.query_urn()),
                                                               Optional.fromNullable(singleContentSelectionCard.style()),
                                                               Optional.fromNullable(singleContentSelectionCard.title()),
                                                               Optional.fromNullable(singleContentSelectionCard.description()),
                                                               mapSelectionItem(selectionItem),
                                                               Optional.fromNullable(singleContentSelectionCard.social_proof()),
                                                               Optional.fromNullable(singleContentSelectionCard.social_proof_avatar_urls()));
    }

    private static DiscoveryCard.MultipleContentSelectionCard mapMultipleContentSelectionCard(DbModel.MultipleContentSelectionCard singleContentSelectionCard,
                                                                                              Collection<DbModel.SelectionItem> selectionItems) {
        return DiscoveryCard.MultipleContentSelectionCard.create(singleContentSelectionCard.urn(),
                                                                 Optional.fromNullable(singleContentSelectionCard.style()),
                                                                 Optional.fromNullable(singleContentSelectionCard.title()),
                                                                 Optional.fromNullable(singleContentSelectionCard.description()),
                                                                 Lists.transform(Lists.newArrayList(selectionItems), DbModelMapper::mapSelectionItem));
    }

    private static SelectionItem mapSelectionItem(DbModel.SelectionItem selectionItem) {
        return SelectionItem.create(Optional.fromNullable(selectionItem.urn()),
                                    Optional.fromNullable(selectionItem.artwork_url_template()),
                                    Optional.fromNullable(selectionItem.count()).transform(Long::intValue),
                                    Optional.fromNullable(selectionItem.short_title()),
                                    Optional.fromNullable(selectionItem.short_subtitle()),
                                    Optional.fromNullable(selectionItem.web_link()),
                                    Optional.fromNullable(selectionItem.app_link()));
    }
}
