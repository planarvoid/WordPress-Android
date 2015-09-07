package com.soundcloud.android.sync;

import static com.soundcloud.android.storage.Table.Collections;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;
import static com.soundcloud.propeller.rx.RxResultMapper.scalar;

import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;

import javax.inject.Inject;
import java.util.Arrays;

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

    public Observable<Boolean> hasSyncedCollectionsBefore() {
        final Query query = Query.count(Collections)
                .whereNotNull(TableColumns.Collections.LAST_SYNC)
                .whereIn(TableColumns.Collections.URI,
                        Arrays.asList(
                                SyncContent.MyLikes.content.uri.toString(),
                                SyncContent.MyPlaylists.content.uri.toString()
                        )
                );
        return propellerRx.query(query).map(hasSyncedMapper(2)).defaultIfEmpty(false);
    }

    private RxResultMapper<Boolean> hasSyncedMapper(final int contentCount) {
        return new RxResultMapper<Boolean>() {
            @Override
            public Boolean map(CursorReader reader) {
                return reader.getInt(0) >= contentCount;
            }
        };
    }

}
