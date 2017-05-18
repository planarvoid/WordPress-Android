package com.soundcloud.android.discovery;

import com.soundcloud.android.discovery.systemplaylist.SystemPlaylistEntity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.MultiMap;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;

import javax.inject.Inject;
import java.util.List;

public class DiscoveryReadableStorage {

    private final DiscoveryDatabase discoveryDatabase;

    @Inject
    DiscoveryReadableStorage(DiscoveryDatabase discoveryDatabase) {
        this.discoveryDatabase = discoveryDatabase;
    }

    Observable<List<DbModel.SelectionItem>> liveSelectionItems() {
        return discoveryDatabase.observeList(DbModel.SelectionItem.FACTORY.selectAllMapper(), SelectionItemModel.TABLE_NAME, DbModel.SelectionItem.FACTORY.selectAll().statement);
    }

    Single<List<DbModel.SelectionItem>> selectionItems() {
        return discoveryDatabase.selectList(DbModel.SelectionItem.FACTORY.selectAll(), DbModel.SelectionItem.FACTORY.selectAllMapper());
    }

    public Maybe<SystemPlaylistEntity> systemPlaylistEntity(final Urn urn) {
        final Maybe<DbModel.SystemPlaylist> systemPlaylistMaybe = discoveryDatabase.selectList(DbModel.SystemPlaylist.FACTORY.selectByUrn(urn),
                                                                                               DbModel.SystemPlaylist.FACTORY.selectByUrnMapper())
                                                                                   .filter(systemPlaylists -> !systemPlaylists.isEmpty())
                                                                                   .map(systemPlaylists -> systemPlaylists.get(0));

        final Maybe<List<Urn>> trackUrns = discoveryDatabase.selectList(DbModel.SystemPlaylistsTracks.FACTORY.selectTrackUrnsForSystemPlaylistUrn(urn),
                                                                        DbModel.SystemPlaylistsTracks.FACTORY.selectTrackUrnsForSystemPlaylistUrnMapper())
                                                            .toMaybe();

        return systemPlaylistMaybe.zipWith(trackUrns, DbModelMapper::mapSystemPlaylist);
    }

    Observable<List<DiscoveryCard>> liveDiscoveryCards() {
        final Observable<List<DbModel.FullDiscoveryCard>> discoveryCardsObservable = discoveryDatabase.observeList(DbModel.FullDiscoveryCard.MAPPER,
                                                                                                                   DiscoveryCardModel.TABLE_NAME,
                                                                                                                   DbModel.DiscoveryCard.FACTORY.selectAll().statement);

        final Observable<MultiMap<Urn, DbModel.SelectionItem>> selectionItemsObservable = discoveryDatabase.observeList(DbModel.SelectionItem.FACTORY.selectAllMapper(),
                                                                                                                        SelectionItemModel.TABLE_NAME,
                                                                                                                        DbModel.SelectionItem.FACTORY.selectAll().statement)
                                                                                                           .map(DbModelMapper::toMultiMap);
        return Observable.combineLatest(discoveryCardsObservable, selectionItemsObservable, DbModelMapper::mapDiscoveryCardsWithSelectionItems).distinct();
    }

    Single<List<DiscoveryCard>> discoveryCards() {
        final Single<List<DbModel.FullDiscoveryCard>> discoCards = discoveryDatabase.selectList(DbModel.DiscoveryCard.SELECT_ALL, DbModel.FullDiscoveryCard.MAPPER);
        final Single<MultiMap<Urn, DbModel.SelectionItem>> selectionItems = discoveryDatabase.selectList(DbModel.SelectionItem.SELECT_ALL, DbModel.SelectionItem.MAPPER)
                                                                                              .map(DbModelMapper::toMultiMap);
        return discoCards.zipWith(selectionItems, DbModelMapper::mapDiscoveryCardsWithSelectionItems);

    }
}
