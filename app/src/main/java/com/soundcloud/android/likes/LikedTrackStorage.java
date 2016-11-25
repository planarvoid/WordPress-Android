package com.soundcloud.android.likes;

import static com.soundcloud.android.likes.LoadLikedTracksCommand.trackLikeQuery;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.query.Query.Order.DESC;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.storage.Tables.OfflineContent;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.PropellerRx;
import rx.Observable;

import javax.inject.Inject;
import java.util.List;

public class LikedTrackStorage {

    private final PropellerRx propellerRx;

    @Inject
    public LikedTrackStorage(PropellerRx propellerRx) {
        this.propellerRx = propellerRx;
    }

    public Observable<List<PropertySet>> loadTrackLikes(int limit, long before) {
        return propellerRx.query(buildTrackLikesQuery(limit, before))
                          .map(new LikedTrackMapper())
                          .toList();
    }

    private Query buildTrackLikesQuery(int limit, long before) {
        return trackLikeQuery()
                .whereLt(Tables.Likes.CREATED_AT, before)
                .order(Tables.Likes.CREATED_AT, DESC)
                .limit(limit);
    }

    public Observable<PropertySet> loadTrackLike(Urn track) {
        return propellerRx.query(buildQuery(track)).map(new LikedTrackMapper());
    }

    private Query buildQuery(Urn input) {
        return trackLikeQuery().whereEq(Tables.Likes._ID, input.getNumericId());
    }

    static Where offlineLikesFilter() {
        return filter()
                .whereEq(OfflineContent._ID, OfflineContent.ID_OFFLINE_LIKES)
                .whereEq(OfflineContent._TYPE, OfflineContent.TYPE_COLLECTION);
    }

}
