package com.soundcloud.android.offline.commands;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.commands.UrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.QueryResult;
import com.soundcloud.propeller.query.Query;

import javax.inject.Inject;
import java.util.List;

public class LoadOfflineContentCommand extends Command<List<Urn>, List<Urn>, LoadOfflineContentCommand> {
    private final PropellerDatabase database;

    @Inject
    public LoadOfflineContentCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public List<Urn> call() throws Exception {
        return loadTracksToDownload().toList(new UrnMapper());
    }

    private QueryResult loadTracksToDownload() {
        return database.query(Query
                .from(Table.TrackDownloads.name())
                .whereNull(TableColumns.TrackDownloads.DOWNLOADED_AT)



                .leftJoin(Table.TrackDownloads.name(), TableColumns.Likes._ID, TableColumns.TrackDownloads._ID)
                .whereEq(Table.Likes + "." + TableColumns.Likes._TYPE, TableColumns.Sounds.TYPE_TRACK)
        );
    }
}
