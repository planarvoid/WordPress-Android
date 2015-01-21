package com.soundcloud.android.offline.commands;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.TableColumns.TrackDownloads.REMOVED_AT;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.commands.UrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;

import javax.inject.Inject;
import java.util.List;

public class LoadDownloadsPendingRemovalCommand extends Command<Long, List<Urn>, LoadDownloadsPendingRemovalCommand> {

    private final PropellerDatabase database;

    @Inject
    LoadDownloadsPendingRemovalCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public List<Urn> call() throws Exception {
        final long removalDelayedTimestamp = System.currentTimeMillis() - input;
        return database.query(Query.from(Table.TrackDownloads.name())
                .select(_ID)
                .whereLe(REMOVED_AT, removalDelayedTimestamp))
                .toList(new UrnMapper());
    }
}
