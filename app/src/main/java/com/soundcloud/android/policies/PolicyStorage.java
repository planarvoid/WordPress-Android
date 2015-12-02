package com.soundcloud.android.policies;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import rx.Observable;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

public class PolicyStorage {

    private final PropellerRx propeller;

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
        this.propeller = propeller;
    }

    Observable<Map<Urn,Boolean>> loadBlockedStati(List urns) {
        return propeller.query(buildPolicyQueries(urns)).toMap(urnSelector, blockedSelector);
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
