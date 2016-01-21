package com.soundcloud.android.offline;

import static com.soundcloud.propeller.query.ColumnFunctions.exists;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.rx.RxResultMapper.scalar;

import com.soundcloud.android.commands.PlaylistUrnMapper;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.PreferenceChangeOnSubscribe;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.storage.Tables.OfflineContent;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Function;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.PropellerRx;
import rx.Observable;
import rx.functions.Func1;

import android.content.ContentValues;
import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class OfflineContentStorage {
    private static final String IS_OFFLINE_COLLECTION = "is_offline_collection";
    private static final String IS_OFFLINE_PLAYLIST = "is_offline_playlist";
    private static final String IS_OFFLINE_LIKES = "if_offline_likes";
    private static final String OFFLINE_CONTENT = "has_content_offline";

    private static final Func1<String, Boolean> IS_OFFLINE_COLLECTION_KEY = new Func1<String, Boolean>() {
        @Override
        public Boolean call(String key) {
            return key.equals(IS_OFFLINE_COLLECTION);
        }
    };

    private final Func1<String, Boolean> toPreferenceValue = new Func1<String, Boolean>() {
        @Override
        public Boolean call(String key) {
            return sharedPreferences.getBoolean(key, false);
        }
    };

    private final PropellerRx propellerRx;
    private final SharedPreferences sharedPreferences;

    @Inject
    public OfflineContentStorage(PropellerRx propellerRx,
                                 @Named(StorageModule.OFFLINE_SETTINGS) SharedPreferences sharedPreferences) {
        this.propellerRx = propellerRx;
        this.sharedPreferences = sharedPreferences;
    }

    public Boolean isOfflineCollectionEnabled() {
        return sharedPreferences.getBoolean(IS_OFFLINE_COLLECTION, false);
    }

    Observable<Boolean> getOfflineCollectionStateChanges() {
        return Observable.create(new PreferenceChangeOnSubscribe(sharedPreferences))
                .filter(IS_OFFLINE_COLLECTION_KEY)
                .map(toPreferenceValue);
    }


    public void storeOfflineCollectionDisabled() {
        sharedPreferences.edit().putBoolean(IS_OFFLINE_COLLECTION, false).apply();
    }

    public void storeOfflineCollectionEnabled() {
        sharedPreferences.edit().putBoolean(IS_OFFLINE_COLLECTION, true).apply();
    }

    public Observable<Boolean> isOfflinePlaylist(Urn playlistUrn) {
        return propellerRx.query(isMarkedForOfflineQuery(playlistUrn)).map(scalar(Boolean.class));
    }

    public Observable<Boolean> isOfflineLikesEnabled() {
        return propellerRx.query(isOfflineLikesEnabledQuery()).map(scalar(Boolean.class));
    }

    public Observable<ChangeResult> storeAsOfflinePlaylist(Urn playlistUrn) {
        return propellerRx.upsert(OfflineContent.TABLE, buildContentValuesForPlaylist(playlistUrn));
    }

    public Observable<ChangeResult> removeFromOfflinePlaylists(Urn playlistUrn) {
        return propellerRx.delete(OfflineContent.TABLE, playlistFilter(playlistUrn));
    }

    public Observable<List<PropertySet>> setOfflinePlaylists(final List<Urn> expectedOfflinePlaylists) {
        return propellerRx
                .query(offlinePlaylists()).map(new PlaylistUrnMapper()).toList()
                .flatMap(updateOfflinePlaylists(expectedOfflinePlaylists));
    }

    private Func1<List<Urn>, Observable<List<PropertySet>>> updateOfflinePlaylists(final List<Urn> expectedOfflinePlaylists) {
        return new Func1<List<Urn>, Observable<List<PropertySet>>>() {
            @Override
            public Observable<List<PropertySet>> call(List<Urn> actualOfflinePlaylists) {
                final List<Urn> addedOfflinePlaylists = subtract(expectedOfflinePlaylists, actualOfflinePlaylists);
                final List<Urn> removedOfflinePlaylists = subtract(actualOfflinePlaylists, expectedOfflinePlaylists);

                return updateOfflinePlaylists(addedOfflinePlaylists, removedOfflinePlaylists);
            }
        };
    }

    private Observable<List<PropertySet>> updateOfflinePlaylists(final List<Urn> playlistsToAdd, final List<Urn> playlistsToRemove) {
        return propellerRx
                .runTransaction(new PropellerDatabase.Transaction() {
                    @Override
                    public void steps(PropellerDatabase propeller) {
                        step(propeller.delete(OfflineContent.TABLE, playlistsFilter(playlistsToRemove)));
                        step(propeller.bulkInsert(OfflineContent.TABLE, buildContentValuesForPlaylist(playlistsToAdd)));
                    }
                })
                .map(toChangeSet(playlistsToAdd, playlistsToRemove));
    }

    private Func1<TxnResult, List<PropertySet>> toChangeSet(final List<Urn> requestedPlaylist, final List<Urn> noOfflinePlaylist) {
        return new Func1<TxnResult, List<PropertySet>>() {
            @Override
            public List<PropertySet> call(TxnResult txnResult) {
                if (txnResult.success()) {
                    final ArrayList<PropertySet> propertySets = new ArrayList<>(requestedPlaylist.size() + noOfflinePlaylist.size());
                    propertySets.addAll(Lists.transform(requestedPlaylist, toPlaylistProperties(OfflineState.REQUESTED)));
                    propertySets.addAll(Lists.transform(noOfflinePlaylist, toPlaylistProperties(OfflineState.NOT_OFFLINE)));
                    return propertySets;
                } else {
                    return Collections.emptyList();
                }
            }
        };
    }

    private Function<Urn, PropertySet> toPlaylistProperties(final OfflineState state) {
        return new Function<Urn, PropertySet>() {
                    @Override
                    public PropertySet apply(Urn playlist) {
                        return PropertySet.from(
                                PlayableProperty.URN.bind(playlist),
                                OfflineProperty.OFFLINE_STATE.bind(state)
                        );
                    }
                };
    }

    private static List<Urn> subtract(List<Urn> items, List<Urn> itemsToSubtract) {
        final ArrayList<Urn> result = new ArrayList<>(items);
        result.removeAll(itemsToSubtract);
        return result;
    }

    private static Query offlinePlaylists() {
        return Query.from(OfflineContent.TABLE).where(offlinePlaylistsFilter());
    }

    public Observable<ChangeResult> storeOfflineLikesDisabled() {
        return propellerRx.delete(OfflineContent.TABLE, offlineLikesFilter());
    }

    public Observable<ChangeResult> storeOfflineLikesEnabled() {
        return propellerRx.upsert(OfflineContent.TABLE, buildContentValuesForOfflineLikes());
    }

    public static Query isOfflineLikesEnabledQuery() {
        return Query.apply(exists(Query.from(OfflineContent.TABLE)
                .where(offlineLikesFilter()))
                .as(IS_OFFLINE_LIKES));
    }

    public boolean hasOfflineContent() {
        return sharedPreferences.getBoolean(OFFLINE_CONTENT, false);
    }

    public void setHasOfflineContent(boolean hasOfflineContent) {
        sharedPreferences.edit().putBoolean(OFFLINE_CONTENT, hasOfflineContent).apply();
    }

    private Query isMarkedForOfflineQuery(Urn playlistUrn) {
        return Query.apply(exists(Query.from(OfflineContent.TABLE)
                .where(playlistFilter(playlistUrn)))
                .as(IS_OFFLINE_PLAYLIST));
    }

    private ContentValues buildContentValuesForPlaylist(Urn playlist) {
        return ContentValuesBuilder.values(2)
                .put(OfflineContent._ID, playlist.getNumericId())
                .put(OfflineContent._TYPE, OfflineContent.TYPE_PLAYLIST)
                .get();
    }

    private List<ContentValues> buildContentValuesForPlaylist(List<Urn> playlists) {
        return Lists.transform(playlists, new Function<Urn, ContentValues>() {
            @Override
            public ContentValues apply(Urn playlist) {
                return buildContentValuesForPlaylist(playlist);
            }
        });
    }

    private ContentValues buildContentValuesForOfflineLikes() {
        return ContentValuesBuilder.values(2)
                .put(OfflineContent._ID, OfflineContent.ID_OFFLINE_LIKES)
                .put(OfflineContent._TYPE, OfflineContent.TYPE_COLLECTION)
                .get();
    }

    static Where offlineLikesFilter() {
        return filter()
                .whereEq(OfflineContent._ID, OfflineContent.ID_OFFLINE_LIKES)
                .whereEq(OfflineContent._TYPE, OfflineContent.TYPE_COLLECTION);
    }

    private static Where offlinePlaylistsFilter() {
        return filter().whereEq(OfflineContent._TYPE, OfflineContent.TYPE_PLAYLIST);
    }

    private Where playlistsFilter(List<Urn> playlists) {
        return filter()
                .whereIn(OfflineContent._ID, Lists.transform(playlists, new Function<Urn, Long>() {
                    @Override
                    public Long apply(Urn playlist) {
                        return playlist.getNumericId();
                    }
                }))
                .whereEq(OfflineContent._TYPE, OfflineContent.TYPE_PLAYLIST);
    }

    private Where playlistFilter(Urn urn) {
        return filter()
                .whereEq(OfflineContent._ID, urn.getNumericId())
                .whereEq(OfflineContent._TYPE, OfflineContent.TYPE_PLAYLIST);
    }
}
