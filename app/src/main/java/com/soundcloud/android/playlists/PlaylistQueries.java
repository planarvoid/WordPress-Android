package com.soundcloud.android.playlists;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.Table.PlaylistTracks;
import static com.soundcloud.android.storage.Table.SoundView;
import static com.soundcloud.android.storage.TableColumns.PlaylistTracks.PLAYLIST_ID;
import static com.soundcloud.android.storage.TableColumns.PlaylistTracks.TRACK_ID;

import com.soundcloud.android.offline.OfflineFilters;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables.OfflineContent;
import com.soundcloud.android.storage.Tables.TrackDownloads;
import com.soundcloud.propeller.query.Query;

public final class PlaylistQueries {

    public static final Query HAS_PENDING_DOWNLOAD_REQUEST_QUERY = Query.from(TrackDownloads.TABLE)
            .select(TrackDownloads._ID.qualifiedName())
            .innerJoin(PlaylistTracks.name(), PlaylistTracks.field(TRACK_ID), TrackDownloads._ID.qualifiedName())
            .joinOn(SoundView.field(TableColumns.SoundView._ID), PlaylistTracks.field(PLAYLIST_ID))
            .where(OfflineFilters.REQUESTED_DOWNLOAD_FILTER);

    public static final Query HAS_DOWNLOADED_OFFLINE_TRACKS_FILTER = Query.from(TrackDownloads.TABLE)
            .select(TrackDownloads._ID.qualifiedName())
            .innerJoin(PlaylistTracks.name(), PlaylistTracks.field(TRACK_ID), TrackDownloads._ID.qualifiedName())
            .joinOn(SoundView.field(TableColumns.SoundView._ID), PlaylistTracks.field(PLAYLIST_ID))
            .where(OfflineFilters.OFFLINE_TRACK_FILTER);

    public static final Query IS_MARKED_FOR_OFFLINE_QUERY = Query
            .from(OfflineContent.TABLE.name(), Table.Sounds.name())
            .joinOn(SoundView.field(_ID), OfflineContent._ID.qualifiedName())
            .whereEq(OfflineContent._TYPE.qualifiedName(), OfflineContent.TYPE_PLAYLIST);
}
