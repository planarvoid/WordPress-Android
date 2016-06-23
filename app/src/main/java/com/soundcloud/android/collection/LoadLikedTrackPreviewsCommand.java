package com.soundcloud.android.collection;

import static com.soundcloud.android.storage.TableColumns.Likes.CREATED_AT;
import static com.soundcloud.propeller.query.Field.field;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.query.Query.Order.DESC;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.TableColumns.Sounds;
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
            .whereEq(Table.Likes.field(TableColumns.Likes._ID), Table.Sounds.field(Sounds._ID))
            .whereEq(Table.Likes.field(TableColumns.Likes._TYPE), Table.Sounds.field(Sounds._TYPE));
    private final PropellerDatabase database;
    private final TrackPreviewMapper mapper = new TrackPreviewMapper();

    @Inject
    LoadLikedTrackPreviewsCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public List<LikedTrackPreview> call(Void ignore) {
        return database.query(Query.from(Table.Likes)
                                   .select(field("Likes._id").as(BaseColumns._ID), Sounds.ARTWORK_URL)
                                   .innerJoin(Table.Sounds, WHERE_TRACKS_EXIST)
                                   .whereEq("Likes." + TableColumns.Likes._TYPE, Sounds.TYPE_TRACK)
                                   .order("Likes." + CREATED_AT, DESC)
                                   .whereNull(Table.Likes.field(TableColumns.Likes.REMOVED_AT)))
                       .toList(mapper);
    }

    private static class TrackPreviewMapper implements ResultMapper<LikedTrackPreview> {

        @Override
        public LikedTrackPreview map(CursorReader reader) {
            final long trackId = reader.getLong(Sounds._ID);
            final String artworkUrl = reader.getString(Sounds.ARTWORK_URL);
            return LikedTrackPreview.create(Urn.forTrack(trackId), artworkUrl);
        }
    }
}
