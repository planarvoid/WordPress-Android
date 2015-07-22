package com.soundcloud.android.likes;

import static com.soundcloud.android.storage.Table.Likes;
import static com.soundcloud.android.storage.TableColumns.Likes.CREATED_AT;
import static com.soundcloud.propeller.query.ColumnFunctions.field;
import static com.soundcloud.propeller.query.Query.Order.DESC;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.PropellerRx;
import rx.Observable;

import android.provider.BaseColumns;

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
                .whereLt(Table.Likes.field(TableColumns.Likes.CREATED_AT), before)
                .order(Likes.field(CREATED_AT), DESC)
                .limit(limit);
    }

    public Observable<PropertySet> loadTrackLike(Urn track) {
        return propellerRx.query(buildQuery(track)).map(new LikedTrackMapper());
    }

    private Query buildQuery(Urn input) {
        return LikedTrackStorage.trackLikeQuery().whereEq(Table.Likes.field(TableColumns.Likes._ID), input.getNumericId());
    }

    static Query trackLikeQuery() {
        final String fullSoundIdColumn = Table.Sounds.field(TableColumns.Sounds._ID);
        return Query.from(Table.Likes.name(), Table.Sounds.name(), Table.Users.name())
                .select(
                        field(fullSoundIdColumn).as(BaseColumns._ID),
                        TableColumns.Sounds.TITLE,
                        TableColumns.Users.USERNAME,
                        TableColumns.Sounds.DURATION,
                        TableColumns.Sounds.PLAYBACK_COUNT,
                        TableColumns.Sounds.LIKES_COUNT,
                        TableColumns.Sounds.SHARING,
                        TableColumns.TrackDownloads.REQUESTED_AT,
                        TableColumns.TrackDownloads.DOWNLOADED_AT,
                        TableColumns.TrackDownloads.UNAVAILABLE_AT,
                        TableColumns.TrackPolicies.SUB_MID_TIER,
                        field(Table.TrackDownloads.field(TableColumns.TrackDownloads.REMOVED_AT)).as(TableColumns.TrackDownloads.REMOVED_AT),
                        field(Table.Likes.field(TableColumns.Likes.CREATED_AT)).as(TableColumns.Likes.CREATED_AT))
                .leftJoin(Table.TrackDownloads.name(), fullSoundIdColumn, Table.TrackDownloads.field(TableColumns.TrackDownloads._ID))
                .leftJoin(Table.TrackPolicies.name(), fullSoundIdColumn, Table.TrackPolicies.field(TableColumns.TrackPolicies.TRACK_ID))
                .joinOn(Table.Likes.field(TableColumns.Likes._ID), fullSoundIdColumn)
                .joinOn(Table.Sounds.field(TableColumns.Sounds.USER_ID), Table.Users.field(TableColumns.Users._ID))
                .whereEq(Table.Likes.field(TableColumns.Likes._TYPE), TableColumns.Sounds.TYPE_TRACK)
                .whereNull(Table.Likes.field(TableColumns.Likes.REMOVED_AT));
    }
}
