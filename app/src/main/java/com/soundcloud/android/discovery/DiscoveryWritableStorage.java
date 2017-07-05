package com.soundcloud.android.discovery;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.discovery.systemplaylist.ApiSystemPlaylist;
import com.soundcloud.android.image.ImageStyle;
import com.soundcloud.android.model.Urn;
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
        final ModelCollection<ApiDiscoveryCard> filteredResult = filterInvalidCards(apiDiscoveryCards);
        insertApiDiscoveryCards(filteredResult.getCollection(), filteredResult.getQueryUrn());
    }

    public void storeSystemPlaylist(final ApiSystemPlaylist systemPlaylist) {
        discoveryDatabase.runInTransaction(() -> {
            deleteSystemPlaylistTracks(systemPlaylist.urn());

            deleteSystemPlaylist(systemPlaylist.urn());

            insertSystemPlaylist(systemPlaylist);

            insertSystemPlaylistTracks(systemPlaylist.urn(), systemPlaylist.tracks());
        });
    }

    private void deleteSystemPlaylistTracks(Urn urn) {
        SystemPlaylistsTracksModel.DeleteBySystemPlaylistUrn deleteTracks = new SystemPlaylistsTracksModel.DeleteBySystemPlaylistUrn(discoveryDatabase.writableDatabase(),
                                                                                                                                     DbModel.SystemPlaylistsTracks.FACTORY);
        deleteTracks.bind(urn);
        discoveryDatabase.updateOrDelete(DbModel.SystemPlaylistsTracks.TABLE_NAME, deleteTracks.program);
    }

    private void deleteSystemPlaylist(Urn urn) {
        SystemPlaylistModel.DeleteByUrn deletePlaylist = new SystemPlaylistModel.DeleteByUrn(discoveryDatabase.writableDatabase(), DbModel.SystemPlaylist.FACTORY);
        deletePlaylist.bind(urn);
        discoveryDatabase.updateOrDelete(DbModel.SystemPlaylistsTracks.TABLE_NAME, deletePlaylist.program);
    }

    private void insertSystemPlaylist(ApiSystemPlaylist systemPlaylist) {
        SystemPlaylistModel.InsertRow insertSystemPlaylistRow = new SystemPlaylistModel.InsertRow(discoveryDatabase.writableDatabase(), DbModel.SystemPlaylist.FACTORY);
        insertSystemPlaylistRow.bind(systemPlaylist.urn(),
                                     systemPlaylist.tracks().getQueryUrn().orNull(),
                                     systemPlaylist.title().orNull(),
                                     systemPlaylist.description().orNull(),
                                     systemPlaylist.artworkUrlTemplate().orNull(),
                                     systemPlaylist.trackingFeatureName().orNull(),
                                     systemPlaylist.lastUpdated().orNull());
        discoveryDatabase.insert(DbModel.SystemPlaylist.TABLE_NAME, insertSystemPlaylistRow.program);
    }

    private void insertSystemPlaylistTracks(Urn systemPlaylistUrn, ModelCollection<ApiTrack> tracks) {
        final List<SQLiteStatement> systemPlaylistTracksInserts = Lists.newArrayList();
        for (int i = 0; i < tracks.getCollection().size(); i++) {

            final SystemPlaylistsTracksModel.InsertRow insertSystemPlaylistTracks =
                    new SystemPlaylistsTracksModel.InsertRow(discoveryDatabase.writableDatabase(), DbModel.SystemPlaylistsTracks.FACTORY);
            insertSystemPlaylistTracks.bind(systemPlaylistUrn, tracks.getCollection().get(i).getUrn(), i);
            systemPlaylistTracksInserts.add(insertSystemPlaylistTracks.program);
        }
        discoveryDatabase.batchInsert(DbModel.SystemPlaylistsTracks.TABLE_NAME, systemPlaylistTracksInserts);
    }

    void insertApiDiscoveryCards(List<ApiDiscoveryCard> discoveryCard, Optional<Urn> pageQueryUrn) {
        discoveryDatabase.runInTransaction(() -> {
            final List<SQLiteStatement> inserts = Lists.newArrayList();
            for (ApiDiscoveryCard card : discoveryCard) {
                final DiscoveryCardModel.InsertRow insertDiscoveryCard = new DiscoveryCardModel.InsertRow(discoveryDatabase.writableDatabase());
                Optional<Long> singleSelectionCardId = card.singleContentSelectionCard().transform(apiCard -> insertSingleContentSelectionCard(apiCard, pageQueryUrn));
                Optional<Long> multipleSelectionCardId = card.multipleContentSelectionCard().transform(apiCard -> insertMultipleContentSelectionCard(apiCard, pageQueryUrn));
                insertDiscoveryCard.bind(singleSelectionCardId.orNull(), multipleSelectionCardId.orNull());
                inserts.add(insertDiscoveryCard.program);
            }
            discoveryDatabase.batchInsert(DbModel.DiscoveryCard.TABLE_NAME, inserts);
        });
    }

    private long insertSingleContentSelectionCard(ApiSingleContentSelectionCard card, Optional<Urn> pageQueryUrn) {
        final DbModel.SingleContentSelectionCard.InsertRow insertRow = new DbModel.SingleContentSelectionCard.InsertRow(discoveryDatabase.writableDatabase(),
                                                                                                                        DbModel.SingleContentSelectionCard.FACTORY);
        insertRow.bind(card.selectionUrn(),
                       card.queryUrn().orNull(),
                       pageQueryUrn.orNull(),
                       card.style().orNull(),
                       card.title().orNull(),
                       card.description().orNull(),
                       card.trackingFeatureName().orNull(),
                       card.socialProof().orNull(),
                       card.socialProofAvatarUrlTemplates());
        final long cardId = discoveryDatabase.insert(DbModel.SingleContentSelectionCard.TABLE_NAME, insertRow.program);
        insertSelectionItem(card.selectionItem(), card.selectionUrn());
        return cardId;
    }

    private long insertMultipleContentSelectionCard(ApiMultipleContentSelectionCard card, Optional<Urn> pageQueryUrn) {
        final DbModel.MultipleContentSelectionCard.InsertRow insertRow = new DbModel.MultipleContentSelectionCard.InsertRow(discoveryDatabase.writableDatabase(),
                                                                                                                            DbModel.MultipleContentSelectionCard.FACTORY);
        insertRow.bind(card.selectionUrn(),
                       card.selectionItems().getQueryUrn().orNull(),
                       pageQueryUrn.orNull(),
                       card.style().orNull(),
                       card.title().orNull(),
                       card.description().orNull(),
                       card.trackingFeatureName().orNull());
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
