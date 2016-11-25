package com.soundcloud.android.likes;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;

import android.provider.BaseColumns;

import javax.inject.Inject;
import java.util.Collection;

public class LoadLikedTracksCommand extends Command<Void, Collection<PropertySet>> {
    private final PropellerDatabase propeller;

    @Inject
    LoadLikedTracksCommand(PropellerDatabase propeller) {
        this.propeller = propeller;
    }

    @Override
    public Collection<PropertySet> call(Void input) {
        return propeller.query(trackLikeQuery()).toList(new LikedTrackMapper());
    }

    static Query trackLikeQuery() {
        return Query.from(Tables.Likes.TABLE, Tables.Sounds.TABLE, Tables.Users.TABLE)
                    .select(
                            Tables.Sounds._ID.as(BaseColumns._ID),
                            Tables.Sounds.TITLE,
                            Tables.Users.USERNAME,
                            Tables.Sounds.SNIPPET_DURATION,
                            Tables.Sounds.FULL_DURATION,
                            Tables.Sounds.PLAYBACK_COUNT,
                            Tables.Sounds.LIKES_COUNT,
                            Tables.Sounds.SHARING,
                            Tables.TrackDownloads.REQUESTED_AT,
                            Tables.TrackDownloads.DOWNLOADED_AT,
                            Tables.TrackDownloads.UNAVAILABLE_AT,
                            Tables.TrackDownloads.REMOVED_AT,
                            Tables.TrackPolicies.BLOCKED,
                            Tables.TrackPolicies.SNIPPED,
                            Tables.TrackPolicies.SUB_MID_TIER,
                            Tables.TrackPolicies.SUB_HIGH_TIER,
                            Tables.Likes.CREATED_AT,
                            Tables.OfflineContent._ID)

                    .leftJoin(Tables.OfflineContent.TABLE, LikedTrackStorage.offlineLikesFilter())
                    .leftJoin(Tables.TrackDownloads.TABLE,
                              Tables.Sounds._ID,
                              Tables.TrackDownloads._ID)
                    .innerJoin(Tables.TrackPolicies.TABLE,
                               Tables.Sounds._ID,
                               Tables.TrackPolicies.TRACK_ID)
                    .joinOn(Tables.Likes._ID.qualifiedName(), Tables.Sounds._ID.qualifiedName())
                    .joinOn(Tables.Sounds.USER_ID.name(), Tables.Users._ID.qualifiedName())
                    .whereEq(Tables.Likes._TYPE, Tables.Sounds.TYPE_TRACK)
                    .whereNull(Tables.Likes.REMOVED_AT);
    }
}
