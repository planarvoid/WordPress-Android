package com.soundcloud.android.offline.commands;

import static com.soundcloud.android.storage.TableColumns.TrackDownloads.DOWNLOADED_AT;
import static com.soundcloud.android.storage.TableColumns.TrackDownloads.REMOVED_AT;
import static com.soundcloud.propeller.query.ColumnFunctions.count;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.storage.Table;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.ResultMapper;
import com.soundcloud.propeller.query.Query;

import javax.inject.Inject;

public class OfflineTrackCountCommand extends Command<Object, Integer, OfflineTrackCountCommand> {

    public static final String COUNT_COLUMN = "TotalDownloaded";
    private final PropellerDatabase database;

    @Inject
    OfflineTrackCountCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public Integer call() throws Exception {
        return database.query(Query.from(Table.TrackDownloads.name())
                .select(count(DOWNLOADED_AT).as(COUNT_COLUMN))
                .whereNotNull(DOWNLOADED_AT)
                .whereNull(REMOVED_AT))
                .toList(new CountMapper()).get(0);
    }

    private static class CountMapper implements ResultMapper<Integer> {
        @Override
        public Integer map(CursorReader cursorReader) {
            return cursorReader.getInt(COUNT_COLUMN);
        }
    }

}