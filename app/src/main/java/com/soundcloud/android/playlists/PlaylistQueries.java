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

import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.query.Query;

public final class PlaylistQueries {

    public static final Query HAS_PENDING_DOWNLOAD_REQUEST_QUERY = Query.from(TrackDownloads.name())
            .select(TrackDownloads.field(_ID))
            .innerJoin(PlaylistTracks.name(), PlaylistTracks.field(TRACK_ID), TrackDownloads.field(_ID))
                    // FIXME : migrate to innerJoin or whereEq.
                    // HELP : For some reason this is not equivalent ot whereEq and the innerJoin won't work.
            .joinOn(SoundView.field(TableColumns.SoundView._ID), PlaylistTracks.field(PLAYLIST_ID))
            .whereNull(TrackDownloads.field(REMOVED_AT))
            .whereNull(TrackDownloads.field(DOWNLOADED_AT))
            .whereNull(TrackDownloads.field(UNAVAILABLE_AT))
            .whereNotNull(TrackDownloads.field(REQUESTED_AT));


    public static final Query IS_MARKED_FOR_OFFLINE_QUERY = Query.from(Table.OfflineContent.name(), Table.Sounds.name())
            .joinOn(SoundView.field(_ID), Table.OfflineContent.field(_ID))
            .whereEq(Table.OfflineContent.field(TableColumns.Likes._TYPE), TableColumns.Sounds.TYPE_PLAYLIST);
}
