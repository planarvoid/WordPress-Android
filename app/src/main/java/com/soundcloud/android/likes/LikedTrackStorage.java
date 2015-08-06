package com.soundcloud.android.likes;

import static com.soundcloud.android.playlists.OfflinePlaylistMapper.IS_MARKED_FOR_OFFLINE;
import static com.soundcloud.android.storage.Table.Likes;
import static com.soundcloud.android.storage.TableColumns.Likes.CREATED_AT;
import static com.soundcloud.propeller.query.ColumnFunctions.field;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.query.Query.Order.DESC;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.TableColumns.OfflineContent;
import com.soundcloud.android.storage.Tables.TrackDownloads;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
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
                        TrackDownloads.REQUESTED_AT,
                        TrackDownloads.DOWNLOADED_AT,
                        TrackDownloads.UNAVAILABLE_AT,
                        TrackDownloads.REMOVED_AT.qualifiedName(),
                        TableColumns.TrackPolicies.SUB_MID_TIER,
                        field(Table.Likes.field(TableColumns.Likes.CREATED_AT)).as(TableColumns.Likes.CREATED_AT),
                        field(Table.OfflineContent.field(OfflineContent._ID)).as(IS_MARKED_FOR_OFFLINE))

                .leftJoin(Table.OfflineContent.name(), offlineLikesFilter())
                .leftJoin(TrackDownloads.TABLE.name(), fullSoundIdColumn, TrackDownloads._ID.qualifiedName())
                .leftJoin(Table.TrackPolicies.name(), fullSoundIdColumn, Table.TrackPolicies.field(TableColumns.TrackPolicies.TRACK_ID))
                .joinOn(Table.Likes.field(TableColumns.Likes._ID), fullSoundIdColumn)
                .joinOn(Table.Sounds.field(TableColumns.Sounds.USER_ID), Table.Users.field(TableColumns.Users._ID))

                .whereEq(Table.Likes.field(TableColumns.Likes._TYPE), TableColumns.Sounds.TYPE_TRACK)
                .whereNull(Table.Likes.field(TableColumns.Likes.REMOVED_AT));
    }

    static Where offlineLikesFilter() {
        return filter()
                .whereEq(Table.OfflineContent.field(OfflineContent._ID), OfflineContent.ID_OFFLINE_LIKES)
                .whereEq(Table.OfflineContent.field(OfflineContent._TYPE), OfflineContent.TYPE_COLLECTION);
    }

}
