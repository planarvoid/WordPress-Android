package com.soundcloud.android.offline;

import static com.soundcloud.propeller.ContentValuesBuilder.values;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.rx.RxResultMapper.scalar;

import com.soundcloud.android.commands.PlaylistUrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.PreferenceChangeOnSubscribe;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.storage.Tables.OfflineContent;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;
import com.soundcloud.propeller.ChangeResult;
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
        return isOfflineLikedTracksEnabledCommand.toObservable(null);
    }

    public Observable<ChangeResult> storeAsOfflinePlaylist(Urn playlistUrn) {
        return propellerRx.upsert(OfflineContent.TABLE, buildContentValuesForPlaylist(playlistUrn));
    }

    Observable<ChangeResult> removePlaylistFromOffline(Urn playlistUrn) {
        return propellerRx.delete(
                OfflineContent.TABLE,
                playlistFilter(playlistUrn)
        );
    }

    public Observable<List<Urn>> loadOfflinePlaylists() {
        return propellerRx.query(offlinePlaylists()).map(new PlaylistUrnMapper()).toList();
    }

    public Observable<TxnResult> addOfflinePlaylists(final List<Urn> expectedOfflinePlaylists) {
        return propellerRx.bulkUpsert(OfflineContent.TABLE, buildContentValuesForPlaylist(expectedOfflinePlaylists));
    }

    private static Query offlinePlaylists() {
        return Query.from(OfflineContent.TABLE).where(offlinePlaylistsFilter());
    }

    public Observable<ChangeResult> deleteLikedTrackCollection() {
        return propellerRx.delete(OfflineContent.TABLE, offlineLikesFilter());
    }

    public Observable<ChangeResult> storeLikedTrackCollection() {
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

    private ContentValues buildContentValuesForPlaylist(Urn playlist) {
        return values(2)
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

    private static Where offlinePlaylistsFilter() {
        return filter().whereEq(OfflineContent._TYPE, OfflineContent.TYPE_PLAYLIST);
    }

    private Where playlistFilter(Urn urn) {
        return filter()
                .whereEq(OfflineContent._ID, urn.getNumericId())
                .whereEq(OfflineContent._TYPE, OfflineContent.TYPE_PLAYLIST);
    }

}
