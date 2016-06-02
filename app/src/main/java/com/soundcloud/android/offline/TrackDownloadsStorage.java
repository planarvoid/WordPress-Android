package com.soundcloud.android.offline;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.Table.Likes;
import static com.soundcloud.android.storage.Table.PlaylistTracks;
import static com.soundcloud.android.storage.TableColumns.Likes.CREATED_AT;
import static com.soundcloud.android.storage.TableColumns.PlaylistTracks.PLAYLIST_ID;
import static com.soundcloud.android.storage.TableColumns.PlaylistTracks.POSITION;
import static com.soundcloud.android.storage.TableColumns.PlaylistTracks.TRACK_ID;
import static com.soundcloud.android.storage.Tables.TrackDownloads;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.query.Query.Order.ASC;
import static com.soundcloud.propeller.query.Query.Order.DESC;

import com.soundcloud.android.commands.TrackUrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.PropellerRx;
import rx.Observable;
import rx.functions.Func1;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

class TrackDownloadsStorage {
    private static final long DELAY_BEFORE_REMOVAL = TimeUnit.MINUTES.toMillis(3);
    private static final int DEFAULT_BATCH_SIZE = 500; // default SQL var limit is 999. Being safe

    private static final Func1<CursorReader, Urn> CURSOR_TO_URN = new Func1<CursorReader, Urn>() {
        @Override
        public Urn call(CursorReader cursorReader) {
            return Urn.forTrack(cursorReader.getLong(TrackDownloads._ID));
        }
    };

    private static final Func1<CursorReader, OfflineState> CURSOR_TO_OFFLINE_STATE = new Func1<CursorReader, OfflineState>() {
        @Override
        public OfflineState call(CursorReader cursorReader) {
            return OfflineStateMapper.fromDates(cursorReader, true);
        }
    };

    private static final Func1<List<OfflineState>, OfflineState> TO_COLLECTION_STATE = new Func1<List<OfflineState>, OfflineState>() {
        @Override
        public OfflineState call(List<OfflineState> offlineStates) {
            return OfflineState.getOfflineState(
                    offlineStates.contains(OfflineState.REQUESTED),
                    offlineStates.contains(OfflineState.DOWNLOADED),
                    offlineStates.contains(OfflineState.UNAVAILABLE)
            );
        }
    };

    private final PropellerDatabase propeller;
    private final PropellerRx propellerRx;
    private final DateProvider dateProvider;

    @Inject
    TrackDownloadsStorage(PropellerDatabase propeller, PropellerRx propellerRx, CurrentDateProvider dateProvider) {
        this.propeller = propeller;
        this.propellerRx = propellerRx;
        this.dateProvider = dateProvider;
    }

    Observable<List<Urn>> playlistTrackUrns(Urn playlistUrn) {
        final Query query = Query.from(TrackDownloads.TABLE)
                .select(TrackDownloads._ID.name())
                .innerJoin(PlaylistTracks.name(), PlaylistTracks.field(TRACK_ID), TrackDownloads._ID.name())
                .whereEq(PlaylistTracks.field(PLAYLIST_ID), playlistUrn.getNumericId())
                .where(OfflineFilters.DOWNLOADED_OFFLINE_TRACK_FILTER)
                .order(PlaylistTracks.field(POSITION), ASC);
        return propellerRx.query(query).map(new TrackUrnMapper()).toList();
    }

    Observable<List<Urn>> likesUrns() {
        final Query query = Query.from(TrackDownloads.TABLE)
                .select(TrackDownloads._ID.qualifiedName())
                .innerJoin(Likes.name(), TrackDownloads._ID.qualifiedName(), Likes.field(_ID))
                .where(OfflineFilters.DOWNLOADED_OFFLINE_TRACK_FILTER)
                .order(Likes.field(CREATED_AT), DESC);

        return propellerRx.query(query).map(new TrackUrnMapper()).toList();
    }

    public Observable<Map<Urn, OfflineState>> getOfflineStates() {
        final Query query = Query.from(TrackDownloads.TABLE)
                .select(TrackDownloads._ID,
                        TrackDownloads.REQUESTED_AT,
                        TrackDownloads.REMOVED_AT,
                        TrackDownloads.DOWNLOADED_AT,
                        TrackDownloads.UNAVAILABLE_AT);
        return propellerRx.query(query).toMap(CURSOR_TO_URN, CURSOR_TO_OFFLINE_STATE);
    }

    public List<Urn> onlyOfflineTracks(List<Urn> tracks) {
        List<Urn> result = new ArrayList<>((tracks.size() / DEFAULT_BATCH_SIZE) + 1);
        for (List<Urn> batch : Lists.partition(tracks, DEFAULT_BATCH_SIZE)) {
            result.addAll(propeller.query(Query.from(TrackDownloads.TABLE)
                    .select(TrackDownloads._ID.as(_ID))
                    .where(OfflineFilters.DOWNLOADED_OFFLINE_TRACK_FILTER)
                    .whereIn(TrackDownloads._ID, Urns.toIds(batch)))
                    .toList(new TrackUrnMapper()));
        }
        return result;
    }

    public Observable<OfflineState> getLikesOfflineState() {
        return propellerRx
                .query(Query
                        .from(TrackDownloads.TABLE)
                        .innerJoin(Likes.name(), likedTrackFilter()))
                .map(CURSOR_TO_OFFLINE_STATE)
                .toList()
                .map(TO_COLLECTION_STATE);
    }

    private Where likedTrackFilter() {
        return filter()
                .whereEq(TrackDownloads._ID.qualifiedName(), Likes.field(_ID))
                .whereEq(TableColumns.Likes._TYPE, TableColumns.Sounds.TYPE_TRACK);
    }

    Observable<List<Urn>> getTracksToRemove() {
        final long removalDelayedTimestamp = dateProvider.getCurrentTime() - DELAY_BEFORE_REMOVAL;
        return propellerRx.query(Query.from(TrackDownloads.TABLE)
                .select(_ID)
                .whereLe(TrackDownloads.REMOVED_AT, removalDelayedTimestamp))
                .map(new TrackUrnMapper())
                .toList();
    }

    WriteResult storeCompletedDownload(DownloadState downloadState) {
        final ContentValues contentValues = ContentValuesBuilder.values(3)
                .put(_ID, downloadState.getTrack().getNumericId())
                .put(TrackDownloads.UNAVAILABLE_AT, null)
                .put(TrackDownloads.DOWNLOADED_AT, downloadState.timestamp)
                .get();

        return propeller.upsert(TrackDownloads.TABLE, contentValues);
    }

    public WriteResult markTrackAsUnavailable(Urn track) {
        final ContentValues contentValues = ContentValuesBuilder.values(1)
                .put(TrackDownloads.UNAVAILABLE_AT, dateProvider.getCurrentTime()).get();

        return propeller.update(TrackDownloads.TABLE, contentValues,
                filter().whereEq(_ID, track.getNumericId()));
    }
}
