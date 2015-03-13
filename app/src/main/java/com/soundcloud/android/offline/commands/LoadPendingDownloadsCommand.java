package com.soundcloud.android.offline.commands;

import static com.soundcloud.android.storage.Table.TrackDownloads;
import static com.soundcloud.android.storage.TableColumns.TrackDownloads.DOWNLOADED_AT;
import static com.soundcloud.android.storage.TableColumns.TrackDownloads.REMOVED_AT;
import static com.soundcloud.android.storage.TableColumns.TrackDownloads.REQUESTED_AT;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.commands.UrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.query.WhereBuilder;

import javax.inject.Inject;
import java.util.List;

public class LoadPendingDownloadsCommand extends Command<Object, List<Urn>, LoadPendingDownloadsCommand> {

    private final PropellerDatabase database;

    @Inject
    LoadPendingDownloadsCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public List<Urn> call() throws Exception {
        final Where isPendingDownloads = new WhereBuilder()
                .whereNull(REMOVED_AT)
                .whereNull(DOWNLOADED_AT)
                .whereNotNull(REQUESTED_AT);
        return database.query(Query.from(TrackDownloads.name()).where(isPendingDownloads)).toList(new UrnMapper());
    }
}
