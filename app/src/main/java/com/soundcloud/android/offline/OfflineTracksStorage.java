package com.soundcloud.android.offline;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.Table.Likes;
import static com.soundcloud.android.storage.Table.PlaylistTracks;
import static com.soundcloud.android.storage.Table.TrackDownloads;
import static com.soundcloud.android.storage.TableColumns.Likes.CREATED_AT;
import static com.soundcloud.android.storage.TableColumns.PlaylistTracks.PLAYLIST_ID;
import static com.soundcloud.android.storage.TableColumns.PlaylistTracks.POSITION;
import static com.soundcloud.android.storage.TableColumns.PlaylistTracks.TRACK_ID;
import static com.soundcloud.android.storage.TableColumns.TrackDownloads.DOWNLOADED_AT;
import static com.soundcloud.android.storage.TableColumns.TrackDownloads.REMOVED_AT;
import static com.soundcloud.android.storage.TableColumns.TrackDownloads.REQUESTED_AT;
import static com.soundcloud.android.storage.TableColumns.TrackDownloads.UNAVAILABLE_AT;

import com.soundcloud.android.commands.UrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import rx.Observable;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.TimeUnit;

class OfflineTracksStorage {
    private static final long DELAY_BEFORE_REMOVAL = TimeUnit.MINUTES.toMillis(3);

    private final PropellerRx scheduler;
    private final DateProvider dateProvider;

    @Inject
    OfflineTracksStorage(PropellerRx scheduler, DateProvider dateProvider) {
        this.scheduler = scheduler;
        this.dateProvider = dateProvider;
    }

    /**
     * Loads offline tracks that are part of a given playlist. The playlist does not need to be offline synced.
     * Tracks can be offline synced as part of a different playlist or as an offline like.
     *
     * @param playlistUrn urn of a playlist to load
     * @return playlist's offline tracks playlist ordered by position in a playlist
     */
    Observable<List<Urn>> playlistTrackUrns(Urn playlistUrn) {
        final Query query = Query.from(TrackDownloads.name())
                .select(TrackDownloads.field(_ID))
                .innerJoin(PlaylistTracks.name(), PlaylistTracks.field(TRACK_ID), TrackDownloads.field(_ID))
                .whereEq(PlaylistTracks.field(PLAYLIST_ID), playlistUrn.getNumericId())
                .whereNotNull(TrackDownloads.field(DOWNLOADED_AT))
                .whereNull(TrackDownloads.field(REMOVED_AT))
                .order(PlaylistTracks.field(POSITION), Query.ORDER_ASC);
        return scheduler.query(query).map(new UrnMapper()).toList();
    }

    /**
     * Loads liked tracks that are offline synced and not marked for removal.
     *
     * @return offline likes ordered by creation date of the like
     */
    Observable<List<Urn>> likesUrns() {
        final Query query = Query.from(TrackDownloads.name())
                .select(TrackDownloads.field(_ID))
                .innerJoin(Likes.name(), TrackDownloads.field(_ID), Likes.field(_ID))
                .whereNotNull(TrackDownloads.field(DOWNLOADED_AT))
                .whereNull(TrackDownloads.field(REMOVED_AT))
                .order(Likes.field(CREATED_AT), Query.ORDER_DESC);

        return scheduler.query(query).map(new UrnMapper()).toList();
    }

    Observable<List<Urn>> pendingLikedTracksUrns() {
        final Query query = Query.from(TrackDownloads.name())
                .select(TrackDownloads.field(_ID))
                .innerJoin(Likes.name(), TrackDownloads.field(_ID), Likes.field(_ID))
                .whereNull(TrackDownloads.field(REMOVED_AT))
                .whereNull(TrackDownloads.field(DOWNLOADED_AT))
                .whereNull(TrackDownloads.field(UNAVAILABLE_AT))
                .whereNotNull(TrackDownloads.field(REQUESTED_AT))
                .whereEq(TableColumns.Likes._TYPE, TableColumns.Sounds.TYPE_TRACK);

        return scheduler.query(query).map(new UrnMapper()).toList();
    }

    Observable<List<Urn>> pendingPlaylistTracksUrns(Urn playlist) {
        final Query query = Query.from(TrackDownloads.name())
                .select(TrackDownloads.field(_ID))
                .innerJoin(PlaylistTracks.name(), PlaylistTracks.field(TRACK_ID), TrackDownloads.field(_ID))
                .whereEq(PlaylistTracks.field(PLAYLIST_ID), playlist.getNumericId())
                .whereNull(TrackDownloads.field(REMOVED_AT))
                .whereNull(TrackDownloads.field(DOWNLOADED_AT))
                .whereNull(TrackDownloads.field(UNAVAILABLE_AT))
                .whereNotNull(TrackDownloads.field(REQUESTED_AT));

        return scheduler.query(query).map(new UrnMapper()).toList();
    }

    Observable<List<Urn>> getTracksToRemove() {
        long removalDelayedTimestamp = dateProvider.getCurrentDate().getTime() - DELAY_BEFORE_REMOVAL;
        return scheduler.query(Query.from(Table.TrackDownloads.name())
                .select(_ID)
                .whereLe(REMOVED_AT, removalDelayedTimestamp))
                .map(new UrnMapper())
                .toList();
    }
}
