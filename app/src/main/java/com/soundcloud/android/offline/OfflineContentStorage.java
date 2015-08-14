package com.soundcloud.android.offline;

import static com.soundcloud.propeller.query.ColumnFunctions.exists;
import static com.soundcloud.propeller.rx.RxResultMapper.scalar;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables.OfflineContent;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.query.Filter;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.PropellerRx;
import rx.Observable;

import android.content.ContentValues;

import javax.inject.Inject;

class OfflineContentStorage {
    private static final String IS_OFFLINE_PLAYLIST = "is_offline_playlist";
    private static final String IS_OFFLINE_LIKES = "if_offline_likes";

    private final PropellerRx propellerRx;

    @Inject
    public OfflineContentStorage(PropellerRx propellerRx) {
        this.propellerRx = propellerRx;
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

    private Query isMarkedForOfflineQuery(Urn playlistUrn) {
        return Query.apply(exists(Query.from(OfflineContent.TABLE)
                .where(playlistFilter(playlistUrn)))
                .as(IS_OFFLINE_PLAYLIST));
    }

    private ContentValues buildContentValuesForPlaylist(Urn urn) {
        return ContentValuesBuilder.values(2)
                .put(OfflineContent._ID, urn.getNumericId())
                .put(OfflineContent._TYPE, OfflineContent.TYPE_PLAYLIST)
                .get();
    }

    private ContentValues buildContentValuesForOfflineLikes() {
        return ContentValuesBuilder.values(2)
                .put(OfflineContent._ID, OfflineContent.ID_OFFLINE_LIKES)
                .put(OfflineContent._TYPE, OfflineContent.TYPE_COLLECTION)
                .get();
    }

    static Where offlineLikesFilter() {
        return Filter.filter()
                .whereEq(OfflineContent._ID, OfflineContent.ID_OFFLINE_LIKES)
                .whereEq(OfflineContent._TYPE, OfflineContent.TYPE_COLLECTION);
    }

    private Where playlistFilter(Urn urn) {
        return Filter.filter()
                .whereEq(OfflineContent._ID, urn.getNumericId())
                .whereEq(OfflineContent._TYPE, OfflineContent.TYPE_PLAYLIST);
    }
}
