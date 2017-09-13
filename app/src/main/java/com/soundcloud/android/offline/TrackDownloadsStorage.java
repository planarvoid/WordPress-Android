package com.soundcloud.android.offline;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.Tables.TrackDownloads;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.TrackUrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.QueryResult;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRxV2;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.annotations.NonNull;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

class TrackDownloadsStorage {
    private static final long DELAY_BEFORE_REMOVAL = TimeUnit.MINUTES.toMillis(3);
    private static final int DEFAULT_BATCH_SIZE = 500; // default SQL var limit is 999. Being safe

    private final PropellerDatabase propeller;
    private final PropellerRxV2 propellerRx;
    private final DateProvider dateProvider;

    @Inject
    TrackDownloadsStorage(PropellerDatabase propeller, PropellerRxV2 propellerRx, CurrentDateProvider dateProvider) {
        this.propeller = propeller;
        this.propellerRx = propellerRx;
        this.dateProvider = dateProvider;
    }

    Single<Map<Urn, OfflineState>> getOfflineStates() {
        final Query query = Query.from(TrackDownloads.TABLE)
                                 .select(TrackDownloads._ID,
                                         TrackDownloads.REQUESTED_AT,
                                         TrackDownloads.REMOVED_AT,
                                         TrackDownloads.DOWNLOADED_AT,
                                         TrackDownloads.UNAVAILABLE_AT);
        return propellerRx.queryResult(query).map(queryResult -> {
            final Map<Urn, OfflineState> result = new HashMap<>();
            for (CursorReader cursorReader : queryResult) {
                result.put(Urn.forTrack(cursorReader.getLong(TrackDownloads._ID)), OfflineStateMapper.fromDates(cursorReader, true));
            }
            return result;
        }).firstOrError();
    }

    List<Urn> onlyOfflineTracks(List<Urn> tracks) {
        List<Urn> result = new ArrayList<>(tracks.size());
        for (List<Urn> batch : Lists.partition(tracks, DEFAULT_BATCH_SIZE)) {
            result.addAll(propeller.query(Query.from(TrackDownloads.TABLE)
                                               .select(TrackDownloads._ID.as(_ID))
                                               .where(OfflineFilters.DOWNLOADED_OFFLINE_TRACK_FILTER)
                                               .whereIn(TrackDownloads._ID, Urns.toIds(batch)))
                                   .toList(new TrackUrnMapper()));
        }
        return result;
    }

    Single<Map<Urn, OfflineState>> getOfflineStates(Collection<Urn> tracks) {
        return Observable.fromIterable(Lists.partition(new ArrayList<>(tracks), DEFAULT_BATCH_SIZE))
                         .flatMap(this::batchQueryOfflineStates)
                         .collect(HashMap::new, Map::putAll);

    }

    private Observable<Map<Urn, OfflineState>> batchQueryOfflineStates(@NonNull List<Urn> urns) {
        return propellerRx.queryResult(Query.from(TrackDownloads.TABLE).whereIn(TrackDownloads._ID, Urns.toIds(urns)))
                          .map(this::offlineStatesMapFromCursor);
    }

    @android.support.annotation.NonNull
    private Map<Urn, OfflineState> offlineStatesMapFromCursor(QueryResult queryResult) {
        HashMap<Urn, OfflineState> offlineStates = new HashMap<>();
        while (queryResult.iterator().hasNext()) {
            CursorReader reader = queryResult.iterator().next();
            offlineStates.put(Urn.forTrack(reader.getLong(_ID)), OfflineStateMapper.fromDates(reader, true));
        }
        return offlineStates;
    }

    Single<List<Urn>> getTracksToRemove() {
        final long removalDelayedTimestamp = dateProvider.getCurrentTime() - DELAY_BEFORE_REMOVAL;
        return propellerRx.queryResult(Query.from(TrackDownloads.TABLE)
                                            .select(_ID)
                                            .whereLe(TrackDownloads.REMOVED_AT, removalDelayedTimestamp))
                          .map(queryResult -> queryResult.toList(new TrackUrnMapper()))
                          .singleOrError();
    }

    WriteResult storeCompletedDownload(DownloadState downloadState) {
        final ContentValues contentValues = ContentValuesBuilder.values(3)
                                                                .put(_ID, downloadState.getTrack().getNumericId())
                                                                .put(TrackDownloads.UNAVAILABLE_AT, null)
                                                                .put(TrackDownloads.DOWNLOADED_AT,
                                                                     downloadState.timestamp)
                                                                .get();

        return propeller.upsert(TrackDownloads.TABLE, contentValues);
    }

    WriteResult markTrackAsUnavailable(Urn track) {
        final ContentValues contentValues = ContentValuesBuilder.values(1)
                                                                .put(TrackDownloads.UNAVAILABLE_AT,
                                                                     dateProvider.getCurrentTime()).get();

        return propeller.update(TrackDownloads.TABLE, contentValues,
                                filter().whereEq(_ID, track.getNumericId()));
    }
}
