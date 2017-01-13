package com.soundcloud.android.playlists;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.Table.PlaylistTracks;
import static com.soundcloud.android.storage.Table.SoundView;
import static com.soundcloud.android.storage.TableColumns.PlaylistTracks.PLAYLIST_ID;
import static com.soundcloud.android.storage.TableColumns.PlaylistTracks.TRACK_ID;
import static com.soundcloud.propeller.query.ColumnFunctions.count;

import com.soundcloud.android.offline.OfflineFilters;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.storage.Tables.OfflineContent;
import com.soundcloud.android.storage.Tables.TrackDownloads;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.propeller.query.Query;

public final class PlaylistQueries {

    public static final Query LOCAL_TRACK_COUNT = Query.from(Table.PlaylistTracks)
                                                       .select(count(PlaylistTracks.field(TRACK_ID)))
                                                       .joinOn(SoundView.field(TableColumns.SoundView._ID),
                                                          PlaylistTracks.field(PLAYLIST_ID));

    public static final Query HAS_PENDING_DOWNLOAD_REQUEST_QUERY = Query.from(TrackDownloads.TABLE)
                                                                        .select(TrackDownloads._ID.qualifiedName())
                                                                        .innerJoin(PlaylistTracks.name(),
                                                                                   PlaylistTracks.field(TRACK_ID),
                                                                                   TrackDownloads._ID.qualifiedName())
                                                                        .joinOn(SoundView.field(TableColumns.SoundView._ID),
                                                                                PlaylistTracks.field(PLAYLIST_ID))
                                                                        .where(OfflineFilters.REQUESTED_DOWNLOAD_FILTER);

    public static final Query HAS_DOWNLOADED_OFFLINE_TRACKS_FILTER = Query.from(TrackDownloads.TABLE)
                                                                          .select(TrackDownloads._ID.qualifiedName())
                                                                          .innerJoin(PlaylistTracks.name(),
                                                                                     PlaylistTracks.field(TRACK_ID),
                                                                                     TrackDownloads._ID.qualifiedName())
                                                                          .joinOn(SoundView.field(TableColumns.SoundView._ID),
                                                                                  PlaylistTracks.field(PLAYLIST_ID))
                                                                          .where(OfflineFilters.DOWNLOADED_OFFLINE_TRACK_FILTER);

    public static final Query HAS_UNAVAILABLE_OFFLINE_TRACKS_FILTER = Query.from(TrackDownloads.TABLE)
                                                                           .select(TrackDownloads._ID.qualifiedName())
                                                                           .innerJoin(PlaylistTracks.name(),
                                                                                      PlaylistTracks.field(TRACK_ID),
                                                                                      TrackDownloads._ID.qualifiedName())
                                                                           .joinOn(SoundView.field(TableColumns.SoundView._ID),
                                                                                   PlaylistTracks.field(PLAYLIST_ID))
                                                                           .where(OfflineFilters.UNAVAILABLE_OFFLINE_TRACK_FILTER);

    public static final Query IS_MARKED_FOR_OFFLINE_QUERY = Query
            .from(OfflineContent.TABLE.name(), Tables.Sounds.TABLE.name())
            .joinOn(SoundView.field(_ID), OfflineContent._ID.qualifiedName())
            .whereEq(OfflineContent._TYPE.qualifiedName(), OfflineContent.TYPE_PLAYLIST);

    public static void addPlaylistFilterToQuery(String filter, Query query) {
        final String sanitized = filter.replaceAll("'","''");
        if (!Strings.EMPTY.equals(sanitized)) {
            query.leftJoin(PlaylistTracks.name(), Tables.PlaylistView.ID.name(), PLAYLIST_ID);
            query.leftJoin(Tables.TrackView.TABLE.name(), PlaylistTracks.field(TRACK_ID), Tables.TrackView.ID.name());
            query.where(Tables.PlaylistView.TITLE + " LIKE '%" + sanitized + "%' OR " +
                                Tables.PlaylistView.USERNAME.name() + " LIKE '%" + sanitized + "%' OR " +
                                Tables.TrackView.TITLE.name() + " LIKE '%" + sanitized + "%' OR " +
                                Tables.TrackView.CREATOR_NAME.name() + " LIKE '%" + sanitized + "%'");
        }
    }
}
