package com.soundcloud.android.discovery;

import com.soundcloud.android.discovery.systemplaylist.SystemPlaylistEntity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.MultiMap;
import io.reactivex.Maybe;
import io.reactivex.Single;

import javax.inject.Inject;
import java.util.List;

public class DiscoveryReadableStorage {

    private final DiscoveryDatabase discoveryDatabase;

    @Inject
    DiscoveryReadableStorage(DiscoveryDatabase discoveryDatabase) {
        this.discoveryDatabase = discoveryDatabase;
    }

    Single<List<DbModel.SelectionItem>> selectionItems() {
        return discoveryDatabase.executeAsyncQuery(DbModel.SelectionItem.FACTORY.selectAll(), DbModel.SelectionItem.FACTORY.selectAllMapper());
    }

    public Maybe<SystemPlaylistEntity> systemPlaylistEntity(final Urn urn) {
        final Maybe<DbModel.SystemPlaylist> systemPlaylistMaybe = discoveryDatabase.executeAsyncQuery(DbModel.SystemPlaylist.FACTORY.selectByUrn(urn),
                                                                                                      DbModel.SystemPlaylist.FACTORY.selectByUrnMapper())
                                                                                   .filter(systemPlaylists -> !systemPlaylists.isEmpty())
                                                                                   .map(systemPlaylists -> systemPlaylists.get(0));

        final Maybe<List<Urn>> trackUrns = discoveryDatabase.executeAsyncQuery(DbModel.SystemPlaylistsTracks.FACTORY.selectTrackUrnsForSystemPlaylistUrn(urn),
                                                                               DbModel.SystemPlaylistsTracks.FACTORY.selectTrackUrnsForSystemPlaylistUrnMapper())
                                                            .toMaybe();

        return systemPlaylistMaybe.zipWith(trackUrns, DbModelMapper::mapSystemPlaylist);
    }

    Maybe<List<DiscoveryCard>> discoveryCards() {
        final Single<List<DbModel.FullDiscoveryCard>> discoCards = discoveryDatabase.executeAsyncQuery(DbModel.DiscoveryCard.SELECT_ALL, DbModel.FullDiscoveryCard.MAPPER);
        final Single<MultiMap<Urn, DbModel.SelectionItem>> selectionItems = discoveryDatabase.executeAsyncQuery(DbModel.SelectionItem.SELECT_ALL, DbModel.SelectionItem.MAPPER)
                                                                                             .map(DbModelMapper::toMultiMap);
        return discoCards.zipWith(selectionItems, DbModelMapper::mapDiscoveryCardsWithSelectionItems).filter(list -> !list.isEmpty());
    }
}
