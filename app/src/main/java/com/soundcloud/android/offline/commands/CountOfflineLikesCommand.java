package com.soundcloud.android.offline.commands;

import static com.soundcloud.android.storage.TableColumns.TrackDownloads.DOWNLOADED_AT;
import static com.soundcloud.android.storage.TableColumns.TrackDownloads.REMOVED_AT;

import com.soundcloud.android.commands.LegacyCommand;
import com.soundcloud.android.storage.Table;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;

import javax.inject.Inject;

public class CountOfflineLikesCommand extends LegacyCommand<Object, Integer, CountOfflineLikesCommand> {

    private final PropellerDatabase database;

    @Inject
    CountOfflineLikesCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public Integer call() throws Exception {
        return database.query(Query.count(Table.TrackDownloads.name())
                .whereNotNull(DOWNLOADED_AT)
                .whereNull(REMOVED_AT))
                .first(Integer.class);
    }

}