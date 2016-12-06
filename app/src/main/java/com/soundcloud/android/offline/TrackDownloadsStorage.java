package com.soundcloud.android.offline;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.Tables.TrackDownloads;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.TrackUrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.storage.Tables.Likes;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.PropellerRx;
import rx.Observable;

import android.content.ContentValues;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

class TrackDownloadsStorage {
    private static final long DELAY_BEFORE_REMOVAL = TimeUnit.MINUTES.toMillis(3);
    private static final int DEFAULT_BATCH_SIZE = 500; // default SQL var limit is 999. Being safe

    private final PropellerDatabase propeller;
    private final PropellerRx propellerRx;
    private final DateProvider dateProvider;

    @Inject
    TrackDownloadsStorage(PropellerDatabase propeller, PropellerRx propellerRx, CurrentDateProvider dateProvider) {
        this.propeller = propeller;
        this.propellerRx = propellerRx;
        this.dateProvider = dateProvider;
    }

    Observable<Map<Urn, OfflineState>> getOfflineStates() {
        final Query query = Query.from(TrackDownloads.TABLE)
                                 .select(TrackDownloads._ID,
                                         TrackDownloads.REQUESTED_AT,
                                         TrackDownloads.REMOVED_AT,
                                         TrackDownloads.DOWNLOADED_AT,
                                         TrackDownloads.UNAVAILABLE_AT);
        return propellerRx.query(query).toMap(cursorReader -> Urn.forTrack(cursorReader.getLong(TrackDownloads._ID)),
                                              cursorReader -> OfflineStateMapper.fromDates(cursorReader, true));
    }

    List<Urn> onlyOfflineTracks(List<Urn> tracks) {
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

    Observable<OfflineState> getLikesOfflineState() {
        return propellerRx
                .query(Query
                               .from(TrackDownloads.TABLE)
                               .innerJoin(Likes.TABLE, likedTrackFilter()))
                .map(cursorReader1 -> OfflineStateMapper.fromDates(cursorReader1, true))
                .toList()
                .map(offlineStates -> OfflineState.getOfflineState(
                        offlineStates.contains(OfflineState.REQUESTED),
                        offlineStates.contains(OfflineState.DOWNLOADED),
                        offlineStates.contains(OfflineState.UNAVAILABLE)
                ));
    }

    private Where likedTrackFilter() {
        return filter()
                .whereEq(TrackDownloads._ID.qualifiedName(), Tables.Likes._ID)
                .whereEq(Tables.Likes._TYPE, Tables.Sounds.TYPE_TRACK);
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
