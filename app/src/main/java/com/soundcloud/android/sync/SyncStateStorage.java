package com.soundcloud.android.sync;

import static com.soundcloud.android.storage.Table.Collections;

import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;

import javax.inject.Inject;

public class SyncStateStorage {

    private final PropellerRx propellerRx;

    private static final RxResultMapper<Boolean> HAS_SYNCED_MAPPER = new RxResultMapper<Boolean>() {
        @Override
        public Boolean map(CursorReader reader) {
            return reader.isNotNull(TableColumns.Collections.LAST_SYNC);
        }
    };

    @Inject
    public SyncStateStorage(PropellerRx propellerRx) {
        this.propellerRx = propellerRx;
    }

    public Observable<Boolean> hasSyncedBefore(SyncContent syncContent) {
        final Query query = Query.from(Collections)
                .select(TableColumns.Collections.LAST_SYNC)
                .whereEq(TableColumns.Collections.URI, syncContent.content.uri.toString());

        return propellerRx.query(query).map(HAS_SYNCED_MAPPER).defaultIfEmpty(false);
    }

}
