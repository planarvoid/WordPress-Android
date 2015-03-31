package com.soundcloud.android.offline;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.Table.OfflineContent;
import static com.soundcloud.android.storage.TableColumns.OfflineContent._TYPE;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;
import static com.soundcloud.propeller.rx.RxResultMapper.scalar;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.query.Filter;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.PropellerRx;
import rx.Observable;

import android.content.ContentValues;

import javax.inject.Inject;

class OfflinePlaylistStorage {
    private static final String IS_OFFLINE_PLAYLIST = "is_offline_playlist";
    private final PropellerRx propellerRx;

    @Inject
    public OfflinePlaylistStorage(PropellerRx propellerRx) {
        this.propellerRx = propellerRx;
    }

    public Observable<Boolean> isOfflinePlaylist(Urn playlistUrn) {
        return propellerRx.query(isMarkedForOfflineQuery(playlistUrn)).map(scalar(Boolean.class));
    }

    public Observable<ChangeResult> storeAsOfflinePlaylist(Urn playlistUrn) {
        return propellerRx.upsert(OfflineContent, buildContentValues(playlistUrn));
    }

    public Observable<ChangeResult> removeFromOfflinePlaylists(Urn playlistUrn) {
        return propellerRx.delete(OfflineContent, buildWhereClause(playlistUrn));
    }

    private Query isMarkedForOfflineQuery(Urn playlistUrn) {
        return Query.apply(exists(Query.from(Table.OfflineContent.name())
                .whereEq(OfflineContent.field(_TYPE), TableColumns.Sounds.TYPE_PLAYLIST)
                .whereEq(OfflineContent.field(_ID), playlistUrn.getNumericId())).as(IS_OFFLINE_PLAYLIST));
    }

    private ContentValues buildContentValues(Urn urn) {
        return ContentValuesBuilder.values(2)
                .put(_ID, urn.getNumericId())
                .put(_TYPE, TableColumns.OfflineContent.TYPE_PLAYLIST)
                .get();
    }

    private Where buildWhereClause(Urn urn) {
        return Filter.filter()
                .whereEq(_ID, urn.getNumericId())
                .whereEq(_TYPE, TableColumns.OfflineContent.TYPE_PLAYLIST);
    }
}
