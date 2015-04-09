package com.soundcloud.android.playlists;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.Table.PlaylistTracks;
import static com.soundcloud.android.storage.Table.SoundView;
import static com.soundcloud.android.storage.Table.TrackDownloads;
import static com.soundcloud.android.storage.TableColumns.PlaylistTracks.PLAYLIST_ID;
import static com.soundcloud.android.storage.TableColumns.PlaylistTracks.TRACK_ID;
import static com.soundcloud.android.storage.TableColumns.TrackDownloads.DOWNLOADED_AT;
import static com.soundcloud.android.storage.TableColumns.TrackDownloads.REMOVED_AT;
import static com.soundcloud.android.storage.TableColumns.TrackDownloads.REQUESTED_AT;
import static com.soundcloud.android.storage.TableColumns.TrackDownloads.UNAVAILABLE_AT;
import static com.soundcloud.propeller.query.ColumnFunctions.count;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;
import static com.soundcloud.propeller.query.ColumnFunctions.field;
import static com.soundcloud.propeller.query.Query.on;

import com.soundcloud.android.commands.PagedQueryCommand;
import com.soundcloud.android.likes.ChronologicalQueryParams;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;

import javax.inject.Inject;

public class LoadPostedPlaylistsCommand extends PagedQueryCommand<ChronologicalQueryParams> {

    @Inject
    LoadPostedPlaylistsCommand(PropellerDatabase database) {
        super(database, new PostedPlaylistMapper());
    }

    @Override
    protected Query buildQuery(ChronologicalQueryParams input) {
        return Query.from(SoundView.name())
                .select(
                        field(SoundView.field(TableColumns.SoundView._ID)).as(TableColumns.SoundView._ID),
                        field(SoundView.field(TableColumns.SoundView.TITLE)).as(TableColumns.SoundView.TITLE),
                        field(SoundView.field(TableColumns.SoundView.USERNAME)).as(TableColumns.SoundView.USERNAME),
                        field(SoundView.field(TableColumns.SoundView.TRACK_COUNT)).as(TableColumns.SoundView.TRACK_COUNT),
                        field(SoundView.field(TableColumns.SoundView.LIKES_COUNT)).as(TableColumns.SoundView.LIKES_COUNT),
                        field(SoundView.field(TableColumns.SoundView.SHARING)).as(TableColumns.SoundView.SHARING),
                        field(SoundView.field(TableColumns.SoundView.CREATED_AT)).as(TableColumns.SoundView.CREATED_AT),
                        count(PLAYLIST_ID).as(PlaylistMapper.LOCAL_TRACK_COUNT),
                        exists(pendingPlaylistTracksUrns()).as(PostedPlaylistMapper.HAS_PENDING_DOWNLOAD_REQUEST),
                        exists(isMarkedForOfflineQuery()).as(PostedPlaylistMapper.IS_MARKED_FOR_OFFLINE))
                .leftJoin(Table.PlaylistTracks.name(), SoundView.field(TableColumns.SoundView._ID), PLAYLIST_ID)
                .innerJoin(Table.Posts.name(),
                        on(Table.Posts.field(TableColumns.Posts.TARGET_ID), SoundView.field(TableColumns.SoundView._ID))
                                .whereEq(Table.Posts.field(TableColumns.Posts.TARGET_TYPE), SoundView.field(TableColumns.SoundView._TYPE)))
                .leftJoin(Table.TrackDownloads.name(), PlaylistTracks.field(TRACK_ID), Table.TrackDownloads.field(TableColumns.TrackDownloads._ID))
                .whereEq(Table.Posts.field(TableColumns.Posts.TYPE), TableColumns.Posts.TYPE_POST)
                .whereEq(Table.Posts.field(TableColumns.Posts.TARGET_TYPE), TableColumns.Sounds.TYPE_PLAYLIST)
                .whereLt(SoundView.field(TableColumns.SoundView.CREATED_AT), input.getTimestamp())
                .groupBy(SoundView.field(TableColumns.SoundView._ID))
                .order(TableColumns.SoundView.CREATED_AT, Query.ORDER_DESC);
    }

    private Query isMarkedForOfflineQuery() {
        return Query.from(Table.OfflineContent.name(), Table.Sounds.name())
                .joinOn(SoundView.field(TableColumns.SoundView._ID), Table.OfflineContent.field(TableColumns.Likes._ID))
                .whereEq(Table.OfflineContent.field(TableColumns.Likes._TYPE), TableColumns.Sounds.TYPE_PLAYLIST);
    }

    private Query pendingPlaylistTracksUrns() {
        return Query.from(TrackDownloads.name())
                .select(TrackDownloads.field(_ID))
                .innerJoin(PlaylistTracks.name(), PlaylistTracks.field(TRACK_ID), TrackDownloads.field(_ID))
                // FIXME : migrate to innerJoin or whereEq.
                // HELP : For some reason this is not equivalent ot whereEq and the innerJoin won't work.
                .joinOn(SoundView.field(TableColumns.SoundView._ID), PlaylistTracks.field(PLAYLIST_ID))
                .whereNull(TrackDownloads.field(REMOVED_AT))
                .whereNull(TrackDownloads.field(DOWNLOADED_AT))
                .whereNull(TrackDownloads.field(UNAVAILABLE_AT))
                .whereNotNull(TrackDownloads.field(REQUESTED_AT));
    }

}
