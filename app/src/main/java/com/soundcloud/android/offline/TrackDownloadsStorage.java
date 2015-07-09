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
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.TrackUrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.TimeUnit;

class TrackDownloadsStorage {
    private static final long DELAY_BEFORE_REMOVAL = TimeUnit.MINUTES.toMillis(3);

    private final PropellerDatabase propeller;
    private final PropellerRx propellerRx;
    private final DateProvider dateProvider;

    @Inject
    TrackDownloadsStorage(PropellerDatabase propeller, PropellerRx propellerRx, DateProvider dateProvider) {
        this.propeller = propeller;
        this.propellerRx = propellerRx;
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
        return propellerRx.query(query).map(new TrackUrnMapper()).toList();
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

        return propellerRx.query(query).map(new TrackUrnMapper()).toList();
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

        return propellerRx.query(query).map(new TrackUrnMapper()).toList();
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

        return propellerRx.query(query).map(new TrackUrnMapper()).toList();
    }

    Observable<List<Urn>> getTracksToRemove() {
        final long removalDelayedTimestamp = dateProvider.getCurrentDate().getTime() - DELAY_BEFORE_REMOVAL;
        return propellerRx.query(Query.from(Table.TrackDownloads.name())
                .select(_ID)
                .whereLe(REMOVED_AT, removalDelayedTimestamp))
                .map(new TrackUrnMapper())
                .toList();
    }

    Observable<Long> getLastPolicyUpdate() {
        return propellerRx.query(Query.from(Table.TrackPolicies.name())
                .select(Table.TrackPolicies.field(TableColumns.TrackPolicies.LAST_UPDATED))
                .innerJoin(Table.TrackDownloads.name(), Table.TrackPolicies.field(TableColumns.TrackPolicies.TRACK_ID), Table.TrackDownloads.field(_ID))
                .order(Table.TrackPolicies.field(TableColumns.TrackPolicies.LAST_UPDATED), Query.ORDER_DESC))
                .map(RxResultMapper.scalar(Long.class))
                .take(1);
    }

    WriteResult storeCompletedDownload(DownloadResult downloadResult) {
        final ContentValues contentValues = ContentValuesBuilder.values(3)
                .put(_ID, downloadResult.getTrack().getNumericId())
                .put(UNAVAILABLE_AT, null)
                .put(DOWNLOADED_AT, downloadResult.timestamp)
                .get();

        return propeller.upsert(TrackDownloads, contentValues);
    }

    public WriteResult markTrackAsUnavailable(Urn track) {
        final ContentValues contentValues = ContentValuesBuilder.values(1)
                .put(UNAVAILABLE_AT, dateProvider.getCurrentDate().getTime()).get();

        return propeller.update(TrackDownloads, contentValues,
                filter().whereEq(_ID, track.getNumericId()));
    }

}
