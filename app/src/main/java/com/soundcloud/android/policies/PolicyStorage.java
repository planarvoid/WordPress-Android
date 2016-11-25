package com.soundcloud.android.policies;

import com.soundcloud.android.commands.TrackUrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
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
    private static final Query TRACKS_FOR_POLICY_UPDATE_QUERY = Query.from(Tables.Sounds.TABLE)
                                                                     .whereEq(Tables.Sounds._TYPE,
                                                                              Tables.Sounds.TYPE_TRACK);

    private final PropellerDatabase propeller;
    private final PropellerRx propellerRx;
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
    public PolicyStorage(PropellerDatabase propeller) {
        this(propeller, DEFAULT_BATCH_SIZE);
    }

    PolicyStorage(PropellerDatabase propeller, int batchSize) {
        this.propeller = propeller;
        this.propellerRx = new PropellerRx(propeller);
        this.batchSize = batchSize;
    }

    Observable<Map<Urn, Boolean>> loadBlockedStatuses(List<Urn> urns) {
        List<Observable<CursorReader>> batches = new ArrayList<>((urns.size() / batchSize) + 1);
        for (List<Urn> batch : Lists.partition(urns, batchSize)) {
            batches.add(propellerRx.query(buildPolicyQueries(batch)));
        }
        return Observable.merge(batches).toMap(urnSelector, blockedSelector);
    }

    private Query buildPolicyQueries(List<Urn> urns) {
        return Query.from(Table.SoundView.name())
                    .select(
                            TableColumns.SoundView._ID,
                            TableColumns.SoundView.POLICIES_BLOCKED
                    )
                    .whereIn(TableColumns.SoundView._ID, Urns.toIds(urns))
                    .whereEq(TableColumns.SoundView._TYPE, Tables.Sounds.TYPE_TRACK);
    }

    List<Urn> loadTracksForPolicyUpdate() {
        return propeller.query(TRACKS_FOR_POLICY_UPDATE_QUERY).toList(new TrackUrnMapper());
    }

    Observable<List<Urn>> tracksForPolicyUpdate() {
        return propellerRx.query(TRACKS_FOR_POLICY_UPDATE_QUERY).map(new TrackUrnMapper()).toList();
    }

}
