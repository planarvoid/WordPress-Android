package com.soundcloud.android.offline.commands;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.LegacyCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.DownloadRequest;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.RxResultMapper;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

public class LoadPrioritizedPendingDownloadsCommand extends LegacyCommand<Object, List<DownloadRequest>, LoadPrioritizedPendingDownloadsCommand> {

    private final PropellerDatabase database;

    @Inject
    LoadPrioritizedPendingDownloadsCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public List<DownloadRequest> call() throws Exception {
        final Collection<DownloadRequest> set = new LinkedHashSet<>();
        set.addAll(tracksFromOfflinePlaylists());
        set.addAll(tracksFromLikes());
        return new ArrayList<>(set);
    }

    private Collection<DownloadRequest> tracksFromLikes() {
        final Query likesToDownload = Query.from(Table.Sounds.name())
                .select(Table.Sounds.field(_ID),
                        Table.Sounds.field(TableColumns.Sounds.STREAM_URL))
                .innerJoin(Table.TrackDownloads.name(), getSoundsJoinTrackDownloads())
                .innerJoin(Table.Likes.name(), Table.Likes.field(_ID), Table.TrackDownloads.field(_ID))
                .order(Table.Likes.field(TableColumns.Likes.CREATED_AT), Query.ORDER_DESC);

        return database.query(likesToDownload).toList(new DownloadRequestMapper());
    }

    private Collection<DownloadRequest> tracksFromOfflinePlaylists() {

        final Query orderedPlaylists = Query.from(Table.OfflineContent.name())
                .select(Table.OfflineContent.field(TableColumns.OfflineContent._ID))
                .innerJoin(Table.Sounds.name(), filter().whereEq(Table.Sounds.field(_ID), Table.OfflineContent.field(_ID)))
                .whereEq(Table.Sounds.field(TableColumns.Sounds._TYPE), TableColumns.Sounds.TYPE_PLAYLIST)
                .order(Table.Sounds.field(TableColumns.Sounds.CREATED_AT), Query.ORDER_DESC);

        final List<Long> playlistIds = database.query(orderedPlaylists).toList(new IdMapper());

        final Query playlistTracksToDownload = Query.from(Table.PlaylistTracks.name())
                .select(Table.Sounds.field(_ID),
                        Table.Sounds.field(TableColumns.Sounds.STREAM_URL))
                .innerJoin(Table.Sounds.name(), filter().whereEq(Table.Sounds.field(_ID), Table.PlaylistTracks.field(TableColumns.PlaylistTracks.TRACK_ID))
                        .whereIn(TableColumns.PlaylistTracks.PLAYLIST_ID, playlistIds))
                .innerJoin(Table.TrackDownloads.name(), getSoundsJoinTrackDownloads())
                //TODO: this should be changed once propeller supports ordering by multiple columns: https://github.com/soundcloud/propeller/issues/51
                .order(Table.PlaylistTracks.field(TableColumns.PlaylistTracks.PLAYLIST_ID), Query.ORDER_DESC + ", " + Table.PlaylistTracks.field(TableColumns.PlaylistTracks.POSITION) + " " + Query.ORDER_ASC);

        return database.query(playlistTracksToDownload).toList(new DownloadRequestMapper());
    }

    private Where getSoundsJoinTrackDownloads() {
        return filter().whereEq(Table.Sounds.field(_ID), Table.TrackDownloads.field(_ID))
                .whereEq(Table.Sounds.field(TableColumns.Sounds._TYPE), TableColumns.Sounds.TYPE_TRACK)
                .whereNull(Table.TrackDownloads.field(TableColumns.TrackDownloads.REMOVED_AT))
                .whereNull(Table.TrackDownloads.field(TableColumns.TrackDownloads.DOWNLOADED_AT));
    }

    private static class DownloadRequestMapper extends RxResultMapper<DownloadRequest> {
        @Override
        public DownloadRequest map(CursorReader reader) {
            return new DownloadRequest(
                    Urn.forTrack(reader.getLong(_ID)),
                    reader.getString(TableColumns.Sounds.STREAM_URL));
        }
    }

    private static class IdMapper extends RxResultMapper<Long> {

        @Override
        public Long map(CursorReader reader) {
            return reader.getLong(_ID);
        }
    }
}
