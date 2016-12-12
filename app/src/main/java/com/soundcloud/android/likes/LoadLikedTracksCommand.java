package com.soundcloud.android.likes;

import static com.soundcloud.propeller.query.Field.field;
import static com.soundcloud.propeller.query.Filter.filter;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.commands.Command;
import com.soundcloud.android.storage.BaseRxResultMapper;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;

import android.provider.BaseColumns;

import javax.inject.Inject;
import java.util.List;

public class LoadLikedTracksCommand extends Command<Optional<LoadLikedTracksCommand.Params>, List<Like>> {
    private final PropellerDatabase propeller;

    @Inject
    LoadLikedTracksCommand(PropellerDatabase propeller) {
        this.propeller = propeller;
    }

    @Override
    public List<Like> call(Optional<Params> input) {
        Query query = trackLikeQuery();
        if (input.isPresent()) {
            query.whereLt(Tables.Likes.CREATED_AT, input.get().beforeTime());
            query.limit(input.get().limit());
        }
        return propeller.query(query).toList(new BaseRxResultMapper<Like>() {
            @Override
            public Like map(CursorReader cursorReader) {
                return Like.create(readTrackUrn(cursorReader),
                            cursorReader.getDateFromTimestamp(Tables.Likes.CREATED_AT));

            }
        });
    }

    private static Query trackLikeQuery() {
        return Query.from(Tables.Likes.TABLE)
                    .select(
                            field("Likes._id").as(BaseColumns._ID),
                            Tables.Likes.CREATED_AT)
                    .innerJoin(Tables.Sounds.TABLE, whereTrackDataExistsFilter())
                    .whereEq(Tables.Likes._TYPE, Tables.Sounds.TYPE_TRACK)
                    .whereNull(Tables.Likes.REMOVED_AT)
                    .order(Tables.Likes.CREATED_AT, Query.Order.DESC);
    }

    private static Where whereTrackDataExistsFilter() {
        return filter()
                    .whereEq(Tables.Likes._ID, Tables.Sounds._ID)
                    .whereEq(Tables.Likes._TYPE, Tables.Sounds._TYPE);
    }

    @AutoValue
    public static abstract class Params {
        abstract long beforeTime();
        abstract int limit();

        public static Params from(long beforeTime, int limit) {
            return new AutoValue_LoadLikedTracksCommand_Params(beforeTime, limit);
        }
    }
}
