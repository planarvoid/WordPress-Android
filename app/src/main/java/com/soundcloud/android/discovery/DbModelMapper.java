package com.soundcloud.android.discovery;

import com.soundcloud.android.discovery.systemplaylist.SystemPlaylistEntity;
import com.soundcloud.android.image.ImageStyle;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.ListMultiMap;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.MultiMap;
import com.soundcloud.java.optional.Optional;

import java.util.Collection;
import java.util.List;

final class DbModelMapper {
    private DbModelMapper() {
    }

    static MultiMap<Urn, DbModel.SelectionItem> toMultiMap(List<DbModel.SelectionItem> list) {
        final MultiMap<Urn, DbModel.SelectionItem> multiMap = new ListMultiMap<>();
        for (DbModel.SelectionItem selectionItem : list) {
            multiMap.put(selectionItem.selection_urn(), selectionItem);
        }
        return multiMap;
    }

    static List<DiscoveryCard> mapDiscoveryCardsWithSelectionItems(List<DbModel.FullDiscoveryCard> fullDiscoveryCards, MultiMap<Urn, DbModel.SelectionItem> selectionItemsMultiMap) {
        return Lists.transform(fullDiscoveryCards, fullDiscoveryCard -> mapDiscoveryCard(fullDiscoveryCard, selectionItemsMultiMap));
    }

    static SystemPlaylistEntity mapSystemPlaylist(DbModel.SystemPlaylist systemPlaylist, List<Urn> trackUrns) {
        return SystemPlaylistEntity.create(systemPlaylist.urn(),
                                           Optional.fromNullable(systemPlaylist.query_urn()),
                                           Optional.fromNullable(systemPlaylist.title()),
                                           Optional.fromNullable(systemPlaylist.description()),
                                           trackUrns,
                                           Optional.fromNullable(systemPlaylist.last_updated()),
                                           Optional.fromNullable(systemPlaylist.artwork_url_template()),
                                           Optional.fromNullable(systemPlaylist.tracking_feature_name())
        );
    }

    private static DiscoveryCard mapDiscoveryCard(DbModel.FullDiscoveryCard fullDiscoveryCard, MultiMap<Urn, DbModel.SelectionItem> selectionItems) {
        if (fullDiscoveryCard.single_content_selection_card() != null) {
            final DbModel.SingleContentSelectionCard selectionCard = fullDiscoveryCard.single_content_selection_card();
            final Collection<DbModel.SelectionItem> itemForCard = selectionItems.get(selectionCard.urn());
            final DbModel.SelectionItem selectionItem = itemForCard.iterator().next();
            return mapSingleContentSelectionCard(selectionCard, selectionItem);
        } else if (fullDiscoveryCard.multiple_content_selection_card() != null) {
            final DbModel.MultipleContentSelectionCard selectionCard = fullDiscoveryCard.multiple_content_selection_card();
            final Collection<DbModel.SelectionItem> itemsForCard = selectionItems.get(selectionCard.urn());
            return mapMultipleContentSelectionCard(selectionCard, itemsForCard);
        } else {
            throw new IllegalArgumentException("Unexpected card type");
        }
    }

    private static DiscoveryCard.SingleContentSelectionCard mapSingleContentSelectionCard(DbModel.SingleContentSelectionCard singleContentSelectionCard, DbModel.SelectionItem selectionItem) {
        return new DiscoveryCard.SingleContentSelectionCard(singleContentSelectionCard.parent_query_urn(),
                                                            singleContentSelectionCard.urn(),
                                                            singleContentSelectionCard.style(),
                                                            singleContentSelectionCard.title(),
                                                            singleContentSelectionCard.description(),
                                                            singleContentSelectionCard.query_urn(),
                                                            mapSelectionItem(singleContentSelectionCard.urn(), selectionItem),
                                                            singleContentSelectionCard.tracking_feature_name(),
                                                            singleContentSelectionCard.social_proof(),
                                                            singleContentSelectionCard.social_proof_avatar_urls());
    }

    private static DiscoveryCard.MultipleContentSelectionCard mapMultipleContentSelectionCard(DbModel.MultipleContentSelectionCard multipleContentSelectionCard,
                                                                                              Collection<DbModel.SelectionItem> selectionItems) {
        return new DiscoveryCard.MultipleContentSelectionCard(multipleContentSelectionCard.parent_query_urn(),
                                                              multipleContentSelectionCard.urn(),
                                                              multipleContentSelectionCard.query_urn(),
                                                              multipleContentSelectionCard.style(),
                                                              multipleContentSelectionCard.title(),
                                                              multipleContentSelectionCard.description(),
                                                              multipleContentSelectionCard.tracking_feature_name(),
                                                              Lists.transform(Lists.newArrayList(selectionItems), item -> DbModelMapper.mapSelectionItem(multipleContentSelectionCard.urn(), item)));
    }

    private static SelectionItem mapSelectionItem(Urn selectionUrn, DbModel.SelectionItem selectionItem) {
        final String artworkStyle = selectionItem.artwork_style();
        final Long count = selectionItem.count();
        return new SelectionItem(selectionItem.urn(),
                                 selectionUrn,
                                 selectionItem.artwork_url_template(),
                                 (artworkStyle != null) ? ImageStyle.fromIdentifier(artworkStyle) : null,
                                 (count != null) ? count.intValue() : null,
                                 selectionItem.short_title(),
                                 selectionItem.short_subtitle(),
                                 selectionItem.web_link(),
                                 selectionItem.app_link());
    }
}
