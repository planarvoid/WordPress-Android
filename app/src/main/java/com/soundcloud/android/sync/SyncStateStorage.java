package com.soundcloud.android.sync;

import static com.soundcloud.android.storage.Table.Collections;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;
import static com.soundcloud.propeller.rx.RxResultMapper.scalar;

import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import rx.Observable;

import android.net.Uri;

import javax.inject.Inject;

public class SyncStateStorage {

    private final PropellerRx propellerRx;

    @Inject
    public SyncStateStorage(PropellerRx propellerRx) {
        this.propellerRx = propellerRx;
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

}
