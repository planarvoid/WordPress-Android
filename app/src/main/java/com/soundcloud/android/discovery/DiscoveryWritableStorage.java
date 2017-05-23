package com.soundcloud.android.discovery;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.image.ImageStyle;
import com.soundcloud.android.discovery.systemplaylist.ApiSystemPlaylist;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;

import android.database.sqlite.SQLiteStatement;

import javax.inject.Inject;
import java.util.List;

public class DiscoveryWritableStorage {

    private final DiscoveryDatabase discoveryDatabase;

    @Inject
    DiscoveryWritableStorage(DiscoveryDatabase discoveryDatabase) {
        this.discoveryDatabase = discoveryDatabase;
    }


    public void clearData() {
        discoveryDatabase.cleanUp();
    }

    void storeDiscoveryCards(ModelCollection<ApiDiscoveryCard> apiDiscoveryCards) {
        clearData();
        insertApiDiscoveryCards(filterInvalidCards(apiDiscoveryCards).getCollection());
    }

    public void storeSystemPlaylist(final ApiSystemPlaylist systemPlaylist) {
        discoveryDatabase.runInTransaction(() -> {

            SystemPlaylistModel.InsertRow insertSystemPlaylistRow = new SystemPlaylistModel.InsertRow(discoveryDatabase.writableDatabase(), DbModel.SystemPlaylist.FACTORY);
            insertSystemPlaylistRow.bind(systemPlaylist.urn(),
                                         systemPlaylist.tracks().getQueryUrn().orNull(),
                                         systemPlaylist.title().orNull(),
                                         systemPlaylist.description().orNull(),
                                         systemPlaylist.artworkUrlTemplate().orNull(),
                                         systemPlaylist.lastUpdated().orNull());
            discoveryDatabase.insert(DbModel.SystemPlaylist.TABLE_NAME, insertSystemPlaylistRow.program);

            final List<SQLiteStatement> systemPlaylistTracksInserts = Lists.newArrayList();
            for (ApiTrack apiTrack : systemPlaylist.tracks()) {
                final SystemPlaylistsTracksModel.InsertRow insertSystemPlaylistTracks =
                        new SystemPlaylistsTracksModel.InsertRow(discoveryDatabase.writableDatabase(), DbModel.SystemPlaylistsTracks.FACTORY);
                insertSystemPlaylistTracks.bind(systemPlaylist.urn(), apiTrack.getUrn());
                systemPlaylistTracksInserts.add(insertSystemPlaylistTracks.program);
            }
            discoveryDatabase.batchInsert(DbModel.SystemPlaylistsTracks.TABLE_NAME, systemPlaylistTracksInserts);
        });
    }

    void insertApiDiscoveryCards(List<ApiDiscoveryCard> discoveryCard) {
        discoveryDatabase.runInTransaction(() -> {
            final List<SQLiteStatement> inserts = Lists.newArrayList();
            for (ApiDiscoveryCard card : discoveryCard) {
                final DiscoveryCardModel.InsertRow insertDiscoveryCard = new DiscoveryCardModel.InsertRow(discoveryDatabase.writableDatabase());
                Optional<Long> singleSelectionCardId = card.singleContentSelectionCard().transform(this::insertSingleContentSelectionCard);
                Optional<Long> multipleSelectionCardId = card.multipleContentSelectionCard().transform(this::insertMultipleContentSelectionCard);
                insertDiscoveryCard.bind(singleSelectionCardId.orNull(), multipleSelectionCardId.orNull());
                inserts.add(insertDiscoveryCard.program);
            }
            discoveryDatabase.batchInsert(DbModel.DiscoveryCard.TABLE_NAME, inserts);
        });
    }

    private long insertSingleContentSelectionCard(ApiSingleContentSelectionCard card) {
        final DbModel.SingleContentSelectionCard.InsertRow insertRow = new DbModel.SingleContentSelectionCard.InsertRow(discoveryDatabase.writableDatabase(),
                                                                                                                        DbModel.SingleContentSelectionCard.FACTORY);
        insertRow.bind(card.selectionUrn(),
                       card.queryUrn().orNull(),
                       card.style().orNull(),
                       card.title().orNull(),
                       card.description().orNull(),
                       card.socialProof().orNull(),
                       card.socialProofAvatarUrlTemplates());
        final long cardId = discoveryDatabase.insert(DbModel.SingleContentSelectionCard.TABLE_NAME, insertRow.program);
        insertSelectionItem(card.selectionItem(), card.selectionUrn());
        return cardId;
    }

    private long insertMultipleContentSelectionCard(ApiMultipleContentSelectionCard card) {
        final DbModel.MultipleContentSelectionCard.InsertRow insertRow = new DbModel.MultipleContentSelectionCard.InsertRow(discoveryDatabase.writableDatabase(),
                                                                                                                            DbModel.MultipleContentSelectionCard.FACTORY);
        insertRow.bind(card.selectionUrn(), card.selectionItems().getQueryUrn().orNull(), card.style().orNull(), card.title().orNull(), card.description().orNull());
        final long cardId = discoveryDatabase.insert(DbModel.MultipleContentSelectionCard.TABLE_NAME, insertRow.program);
        for (ApiSelectionItem apiSelectionItem : card.selectionItems().getCollection()) {
            insertSelectionItem(apiSelectionItem, card.selectionUrn());
        }
        return cardId;
    }

    long insertSelectionItem(ApiSelectionItem selectionItem, Urn cardUrn) {
        final DbModel.SelectionItem.InsertRow insertRow = new SelectionItemModel.InsertRow(discoveryDatabase.writableDatabase(), DbModel.SelectionItem.FACTORY);
        insertRow.bind(selectionItem.urn().orNull(),
                       cardUrn,
                       selectionItem.artworkUrlTemplate().orNull(),
                       selectionItem.artworkStyle().transform(ImageStyle::toIdentifier).orNull(),
                       selectionItem.count().transform(Long::valueOf).orNull(),
                       selectionItem.shortTitle().orNull(),
                       selectionItem.shortSubtitle().orNull(),
                       selectionItem.webLink().orNull(),
                       selectionItem.appLink().orNull());
        return discoveryDatabase.insert(DbModel.SelectionItem.TABLE_NAME, insertRow.program);
    }

    private ModelCollection<ApiDiscoveryCard> filterInvalidCards(ModelCollection<ApiDiscoveryCard> apiDiscoveryCards) {
        return apiDiscoveryCards.copyWithItems(Lists.newArrayList(Iterables.filter(apiDiscoveryCards.getCollection(), this::isValidDiscoveryCard)));
    }

    private boolean isValidDiscoveryCard(ApiDiscoveryCard apiDiscoveryCard) {
        return apiDiscoveryCard.multipleContentSelectionCard().isPresent() || apiDiscoveryCard.singleContentSelectionCard().isPresent();
    }
}
