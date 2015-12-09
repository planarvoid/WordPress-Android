package com.soundcloud.android.policies;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import rx.Observable;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PolicyStorage {

    private static final int DEFAULT_BATCH_SIZE = 500; // default SQL var limit is 999. Being safe
    private final PropellerRx propeller;
    private final int batchSize;

    private final Func1<CursorReader, Urn> urnSelector = new Func1<CursorReader, Urn>() {
        @Override
        public Urn call(CursorReader cursorReader) {
            return Urn.forTrack(cursorReader.getLong(TableColumns.SoundView._ID));
        }
    };

    private final Func1<CursorReader, Boolean> blockedSelector = new Func1<CursorReader, Boolean>() {
        @Override
        public Boolean call(CursorReader cursorReader) {
            return cursorReader.getBoolean(TableColumns.SoundView.POLICIES_BLOCKED);
        }
    };

    @Inject
    public PolicyStorage(PropellerRx propeller) {
        this(propeller, DEFAULT_BATCH_SIZE);
    }

    PolicyStorage(PropellerRx propeller, int batchSize) {
        this.propeller = propeller;
        this.batchSize = batchSize;
    }

    Observable<Map<Urn,Boolean>> loadBlockedStati(List<Urn> urns) {
        List<Observable<CursorReader>> batches = new ArrayList<>((urns.size() / batchSize) + 1);
        for (List<Urn> batch : Lists.partition(urns, batchSize)) {
            batches.add(propeller.query(buildPolicyQueries(batch)));
        }
        return Observable.merge(batches).toMap(urnSelector, blockedSelector);
    }

    private Query buildPolicyQueries(List urns) {
        return Query.from(Table.SoundView.name())
                .select(
                        TableColumns.SoundView._ID,
                        TableColumns.SoundView.POLICIES_BLOCKED
                )
                .whereIn(TableColumns.SoundView._ID, Urns.toIds(urns))
                .whereEq(TableColumns.SoundView._TYPE, TableColumns.Sounds.TYPE_TRACK);
    }
}
