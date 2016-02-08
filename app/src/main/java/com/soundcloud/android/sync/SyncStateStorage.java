package com.soundcloud.android.sync;

import static com.soundcloud.android.storage.Table.Collections;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;
import static com.soundcloud.propeller.rx.RxResultMapper.scalar;

import com.soundcloud.android.Consts;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import rx.Observable;

import android.content.SharedPreferences;
import android.net.Uri;

import javax.inject.Inject;
import javax.inject.Named;

public class SyncStateStorage {

    private final PropellerRx propellerRx;
    private final SharedPreferences preferences;
    private final CurrentDateProvider dateProvider;

    @Inject
    public SyncStateStorage(PropellerRx propellerRx,
                            @Named(StorageModule.SYNCER) SharedPreferences preferences,
                            CurrentDateProvider dateProvider) {
        this.propellerRx = propellerRx;
        this.preferences = preferences;
        this.dateProvider = dateProvider;
    }

    public Observable<Boolean> hasSyncedMyPostsBefore() {
        final Query query  = Query.apply(exists(Query.from(Collections)
                .whereEq(TableColumns.Collections.URI, SyncContent.MySounds.content.uri.toString())
                .whereNotNull(TableColumns.Collections.LAST_SYNC)));

        return propellerRx.query(query).map(scalar(Boolean.class));
    }

    public Observable<Boolean> hasSyncedBefore(Uri uri) {
        final Query query = Query.count(Collections)
                .whereNotNull(TableColumns.Collections.LAST_SYNC)
                .whereIn(TableColumns.Collections.URI, uri.toString());
        return propellerRx.query(query).map(scalar(Boolean.class)).defaultIfEmpty(false);
    }

    public Observable<Long> getLastSync(Uri uri) {
        final Query query = Query.from(Collections)
                .select(TableColumns.Collections.LAST_SYNC)
                .whereIn(TableColumns.Collections.URI, uri.toString());
        return propellerRx.query(query).map(scalar(Long.class)).defaultIfEmpty((long) Consts.NOT_SET);
    }

    public void synced(String entity) {
        preferences.edit().putLong(entity, dateProvider.getCurrentTime()).apply();
    }

    public boolean hasSyncedBefore(String entity) {
        return preferences.getLong(entity, Consts.NOT_SET) != Consts.NOT_SET;
    }
}
