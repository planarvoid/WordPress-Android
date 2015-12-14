package com.soundcloud.android.reporting;

import static com.soundcloud.propeller.query.Query.count;

import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns.Sounds;
import com.soundcloud.propeller.PropellerDatabase;

import javax.inject.Inject;

public class DatabaseReporting {

    private final PropellerDatabase propeller;

    @Inject
    public DatabaseReporting(PropellerDatabase propeller) {
        this.propeller = propeller;
    }

    public int countTracks() {
        return propeller.query(count(Table.Sounds)
                .whereEq(Sounds._TYPE, Sounds.TYPE_TRACK))
                .firstOrDefault(Integer.class, 0);
    }
}
