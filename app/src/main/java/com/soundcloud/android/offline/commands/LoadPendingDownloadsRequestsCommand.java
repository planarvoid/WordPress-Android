package com.soundcloud.android.offline.commands;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.Table.TrackDownloads;
import static com.soundcloud.android.storage.TableColumns.TrackDownloads.DOWNLOADED_AT;
import static com.soundcloud.android.storage.TableColumns.TrackDownloads.REMOVED_AT;

import com.soundcloud.android.commands.LegacyCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.DownloadRequest;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.RxResultMapper;

import javax.inject.Inject;
import java.util.List;

public class LoadPendingDownloadsRequestsCommand extends LegacyCommand<Object, List<DownloadRequest>, LoadPendingDownloadsRequestsCommand> {

    private final PropellerDatabase database;

    @Inject
    LoadPendingDownloadsRequestsCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public List<DownloadRequest> call() throws Exception {
        return database.query(Query.from(TrackDownloads.name(), Table.Sounds.name())
                .joinOn(Table.Sounds + "." + TableColumns.Sounds._ID, TrackDownloads + "." + _ID)
                .select(TrackDownloads + "." + _ID, TableColumns.SoundView.STREAM_URL)
                .whereEq(Table.Sounds + "." + TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_TRACK)
                .whereNull(TrackDownloads + "." + REMOVED_AT)
                .whereNull(DOWNLOADED_AT))
                .toList(new DownloadRequestMapper());
    }

    private static class DownloadRequestMapper extends RxResultMapper<DownloadRequest> {
        @Override
        public DownloadRequest map(CursorReader reader) {
            return new DownloadRequest(
                    Urn.forTrack(reader.getLong(_ID)),
                    reader.getString(TableColumns.SoundView.STREAM_URL));
        }
    }
}
