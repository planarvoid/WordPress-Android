package com.soundcloud.android.offline.commands;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.Table.Likes;
import static com.soundcloud.android.storage.Table.TrackDownloads;
import static com.soundcloud.android.storage.TableColumns.TrackDownloads.DOWNLOADED_AT;
import static com.soundcloud.android.storage.TableColumns.TrackDownloads.REMOVED_AT;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.commands.UrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;

import javax.inject.Inject;
import java.util.List;

public class LoadOfflineTrackUrnsCommand extends Command<Void, List<Urn>, LoadOfflineTrackUrnsCommand> {

    private final PropellerDatabase database;

    @Inject
    public LoadOfflineTrackUrnsCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public List<Urn> call() throws Exception {
        return database.query(Query.from(TrackDownloads.name())
                .select(TrackDownloads + "." + _ID)
                .innerJoin(Likes.name(), TrackDownloads + "." + _ID, Table.Likes + "." + _ID)
                .whereNotNull(DOWNLOADED_AT)
                .whereNull(TrackDownloads + "." + REMOVED_AT)
                .order(Likes + "." + TableColumns.Likes.CREATED_AT, Query.ORDER_DESC))
                .toList(new UrnMapper());
    }
}
