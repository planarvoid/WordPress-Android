package com.soundcloud.android.offline;

import static com.soundcloud.propeller.ContentValuesBuilder.values;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.rx.RxResultMapper.scalar;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.storage.Tables.OfflineContent;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.schema.BulkInsertValues;
import rx.Observable;

import android.content.ContentValues;
import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.List;

class OfflineContentStorage {
    private static final String IS_OFFLINE_COLLECTION = "is_offline_collection";
    private static final String IS_OFFLINE_PLAYLIST = "is_offline_playlist";
    private static final String OFFLINE_CONTENT = "has_content_offline";

    private final PropellerRx propellerRx;
    private final SharedPreferences sharedPreferences;
    private final IsOfflineLikedTracksEnabledCommand isOfflineLikedTracksEnabledCommand;

    @Inject
    public OfflineContentStorage(PropellerRx propellerRx,
                                 @Named(StorageModule.OFFLINE_SETTINGS) SharedPreferences sharedPreferences,
                                 IsOfflineLikedTracksEnabledCommand isOfflineLikedTracksEnabledCommand) {
        this.propellerRx = propellerRx;
        this.sharedPreferences = sharedPreferences;
        this.isOfflineLikedTracksEnabledCommand = isOfflineLikedTracksEnabledCommand;
    }

    public Boolean isOfflineCollectionEnabled() {
        return sharedPreferences.getBoolean(IS_OFFLINE_COLLECTION, false);
    }

    public void removeOfflineCollection() {
        sharedPreferences.edit().putBoolean(IS_OFFLINE_COLLECTION, false).apply();
    }

    public void addOfflineCollection() {
        sharedPreferences.edit().putBoolean(IS_OFFLINE_COLLECTION, true).apply();
    }

    public Observable<Boolean> isOfflinePlaylist(Urn playlistUrn) {
        return propellerRx.query(isMarkedForOfflineQuery(playlistUrn)).map(scalar(Boolean.class));
    }

    public Observable<Boolean> isOfflineLikesEnabled() {
        return isOfflineLikedTracksEnabledCommand.toObservable(null);
    }

    public Observable<TxnResult> storeAsOfflinePlaylists(final List<Urn> playlistUrns) {
        return propellerRx.bulkUpsert(OfflineContent.TABLE, buildContentValuesForPlaylist(playlistUrns));
    }

    Observable<ChangeResult> removePlaylistsFromOffline(List<Urn> playlistUrns) {
        return propellerRx.delete(
                OfflineContent.TABLE,
                playlistFilter(playlistUrns)
        );
    }

    public Observable<TxnResult> resetOfflinePlaylists(final List<Urn> expectedOfflinePlaylists) {
        return propellerRx.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                step(propeller.delete(OfflineContent.TABLE, offlinePlaylistsFilter()));
                step(propeller.bulkInsert(OfflineContent.TABLE,
                                          buildBulkValuesForPlaylists(expectedOfflinePlaylists)));
            }
        });
    }

    public Observable<ChangeResult> removeLikedTrackCollection() {
        return propellerRx.delete(OfflineContent.TABLE, offlineLikesFilter());
    }

    public Observable<ChangeResult> addLikedTrackCollection() {
        return propellerRx.upsert(OfflineContent.TABLE, buildContentValuesForOfflineLikes());
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

    private BulkInsertValues buildBulkValuesForPlaylists(List<Urn> expectedOfflinePlaylists) {
        BulkInsertValues.Builder builder = new BulkInsertValues.Builder(
                Arrays.asList(
                        OfflineContent._ID,
                        OfflineContent._TYPE
                )
        );
        for (Urn playlist : expectedOfflinePlaylists) {
            builder.addRow(Arrays.asList(
                    playlist.getNumericId(),
                    OfflineContent.TYPE_PLAYLIST
            ));
        }
        return builder.build();
    }

    private ContentValues buildContentValuesForPlaylist(Urn playlist) {
        return values(2)
                .put(OfflineContent._ID, playlist.getNumericId())
                .put(OfflineContent._TYPE, OfflineContent.TYPE_PLAYLIST)
                .get();
    }

    private List<ContentValues> buildContentValuesForPlaylist(List<Urn> playlists) {
        return Lists.transform(playlists, playlist -> buildContentValuesForPlaylist(playlist));
    }

    private ContentValues buildContentValuesForOfflineLikes() {
        return values(2)
                .put(OfflineContent._ID, OfflineContent.ID_OFFLINE_LIKES)
                .put(OfflineContent._TYPE, OfflineContent.TYPE_COLLECTION)
                .get();
    }

    static Where offlineLikesFilter() {
        return filter()
                .whereEq(OfflineContent._ID, OfflineContent.ID_OFFLINE_LIKES)
                .whereEq(OfflineContent._TYPE, OfflineContent.TYPE_COLLECTION);
    }

    private Where offlinePlaylistsFilter() {
        return filter().whereEq(OfflineContent._TYPE, OfflineContent.TYPE_PLAYLIST);
    }

    private Where playlistFilter(Urn urn) {
        return filter()
                .whereEq(OfflineContent._ID, urn.getNumericId())
                .whereEq(OfflineContent._TYPE, OfflineContent.TYPE_PLAYLIST);
    }

    private Where playlistFilter(List<Urn> urns) {
        return filter()
                .whereIn(OfflineContent._ID, Urns.toIds(urns))
                .whereEq(OfflineContent._TYPE, OfflineContent.TYPE_PLAYLIST);
    }

}
