package com.soundcloud.android.collection;

import static com.soundcloud.propeller.query.Field.field;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.query.Query.Order.DESC;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.ResultMapper;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;

import android.provider.BaseColumns;

import javax.inject.Inject;
import java.util.List;

class LoadLikedTrackPreviewsCommand extends Command<Void, List<LikedTrackPreview>> {

    private static final Where WHERE_TRACKS_EXIST = filter()
            .whereEq(Tables.Likes._ID, Tables.Sounds._ID)
            .whereEq(Tables.Likes._TYPE, Tables.Sounds._TYPE);
    private final PropellerDatabase database;
    private final TrackPreviewMapper mapper = new TrackPreviewMapper();

    @Inject
    LoadLikedTrackPreviewsCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public List<LikedTrackPreview> call(Void ignore) {
        return database.query(Query.from(Tables.Likes.TABLE)
                                   .select(field("Likes._id").as(BaseColumns._ID), Tables.Sounds.ARTWORK_URL)
                                   .innerJoin(Tables.Sounds.TABLE, WHERE_TRACKS_EXIST)
                                   .whereEq(Tables.Likes._TYPE, Tables.Sounds.TYPE_TRACK)
                                   .order(Tables.Likes.CREATED_AT, DESC)
                                   .whereNull(Tables.Likes.REMOVED_AT))
                       .toList(mapper);
    }

    private static class TrackPreviewMapper implements ResultMapper<LikedTrackPreview> {

        @Override
        public LikedTrackPreview map(CursorReader reader) {
            final long trackId = reader.getLong(Tables.Sounds._ID.name());
            final String artworkUrl = reader.getString(Tables.Sounds.ARTWORK_URL);
            return LikedTrackPreview.create(Urn.forTrack(trackId), artworkUrl);
        }
    }
}
