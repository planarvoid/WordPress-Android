package com.soundcloud.android.offline.commands;

import static com.soundcloud.android.storage.Table.TrackDownloads;
import static com.soundcloud.android.storage.TableColumns.TrackDownloads.DOWNLOADED_AT;

import com.soundcloud.android.commands.LegacyCommand;
import com.soundcloud.android.commands.UrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;

import javax.inject.Inject;
import java.util.List;

public class LoadDownloadedCommand extends LegacyCommand<Object, List<Urn>, LoadDownloadedCommand> {

    private final PropellerDatabase database;

    @Inject
    LoadDownloadedCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public List<Urn> call() throws Exception {
        return database.query(Query.from(TrackDownloads.name()).whereNotNull(DOWNLOADED_AT)).toList(new UrnMapper());
    }
}
