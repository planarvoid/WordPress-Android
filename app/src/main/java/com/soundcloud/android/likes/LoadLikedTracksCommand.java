package com.soundcloud.android.likes;

import static com.soundcloud.propeller.query.Field.field;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
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
        final String fullSoundIdColumn = Table.Sounds.field(TableColumns.Sounds._ID);
        return Query.from(Table.Likes.name(), Table.Sounds.name(), Table.Users.name())
                .select(
                        field(fullSoundIdColumn).as(BaseColumns._ID),
                        TableColumns.Sounds.TITLE,
                        TableColumns.Users.USERNAME,
                        TableColumns.Sounds.DURATION,
                        TableColumns.Sounds.FULL_DURATION,
                        TableColumns.Sounds.PLAYBACK_COUNT,
                        TableColumns.Sounds.LIKES_COUNT,
                        TableColumns.Sounds.SHARING,
                        Tables.TrackDownloads.REQUESTED_AT,
                        Tables.TrackDownloads.DOWNLOADED_AT,
                        Tables.TrackDownloads.UNAVAILABLE_AT,
                        Tables.TrackDownloads.REMOVED_AT,
                        TableColumns.TrackPolicies.SUB_HIGH_TIER,
                        field(Table.Likes.field(TableColumns.Likes.CREATED_AT)).as(TableColumns.Likes.CREATED_AT),
                        Tables.OfflineContent._ID)

                .leftJoin(Tables.OfflineContent.TABLE, LikedTrackStorage.offlineLikesFilter())
                .leftJoin(Tables.TrackDownloads.TABLE.name(), fullSoundIdColumn, Tables.TrackDownloads._ID.qualifiedName())
                .leftJoin(Table.TrackPolicies.name(), fullSoundIdColumn, Table.TrackPolicies.field(TableColumns.TrackPolicies.TRACK_ID))
                .joinOn(Table.Likes.field(TableColumns.Likes._ID), fullSoundIdColumn)
                .joinOn(Table.Sounds.field(TableColumns.Sounds.USER_ID), Table.Users.field(TableColumns.Users._ID))

                .whereEq(Table.Likes.field(TableColumns.Likes._TYPE), TableColumns.Sounds.TYPE_TRACK)
                .whereNull(Table.Likes.field(TableColumns.Likes.REMOVED_AT));
    }
}
