package com.soundcloud.android.reporting;

import static com.soundcloud.propeller.query.Query.count;

import com.soundcloud.android.events.DatabaseMigrationEvent;
import com.soundcloud.android.storage.DatabaseManager;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns.Sounds;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.PropellerDatabase;

import javax.inject.Inject;

public class DatabaseReporting {

    private final PropellerDatabase propeller;
    private final DatabaseManager manager;

    @Inject
    public DatabaseReporting(PropellerDatabase propeller, DatabaseManager manager) {
        this.propeller = propeller;
        this.manager = manager;
    }

    public int countTracks() {
        return propeller.query(count(Table.Sounds)
                                       .whereEq(Sounds._TYPE, Sounds.TYPE_TRACK))
                        .firstOrDefault(Integer.class, 0);
    }

    public Optional<DatabaseMigrationEvent> pullDatabaseMigrationEvent() {
        return Optional.fromNullable(manager.pullMigrationReport());
    }
}
