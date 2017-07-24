package com.soundcloud.android.likes;

import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.offline.OfflineStateMapper;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRxV2;
import io.reactivex.Single;

import javax.inject.Inject;
import java.util.List;

public class LikesOfflineStateStorage {
    private final PropellerRxV2 propellerRx;

    @Inject
    LikesOfflineStateStorage(PropellerRxV2 propellerRx) {
        this.propellerRx = propellerRx;
    }

    public Single<List<OfflineState>> loadLikedTrackOfflineState() {
        return propellerRx.queryResult(trackLikeOfflineStateQuery())
                          .map(queryResult -> queryResult.toList(reader -> OfflineStateMapper.fromDates(reader, true)))
                          .singleOrError();
    }

    private static Query trackLikeOfflineStateQuery() {
        return Query.from(Tables.Likes.TABLE)
                    .select(Tables.TrackDownloads.REQUESTED_AT,
                            Tables.TrackDownloads.DOWNLOADED_AT,
                            Tables.TrackDownloads.UNAVAILABLE_AT,
                            Tables.TrackDownloads.REMOVED_AT)
                    .leftJoin(Tables.TrackDownloads.TABLE,
                              Tables.Likes._ID,
                              Tables.TrackDownloads._ID)
                    .whereEq(Tables.Likes._TYPE, Tables.Sounds.TYPE_TRACK)
                    .whereNull(Tables.Likes.REMOVED_AT);
    }
}
