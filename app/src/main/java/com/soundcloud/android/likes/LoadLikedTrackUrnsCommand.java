package com.soundcloud.android.likes;

import static com.soundcloud.propeller.query.Field.field;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.query.Query.Order.DESC;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.commands.TrackUrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;

import android.provider.BaseColumns;

import javax.inject.Inject;
import java.util.List;

public class LoadLikedTrackUrnsCommand extends Command<Void, List<Urn>> {

    private final PropellerDatabase database;

    @Inject
    LoadLikedTrackUrnsCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public List<Urn> call(Void input) {
        final Where whereTrackDataExists = filter()
                .whereEq(Tables.Likes._ID, Tables.Sounds._ID)
                .whereEq(Tables.Likes._TYPE, Tables.Sounds._TYPE);

        return database.query(Query.from(Tables.Likes.TABLE)
                                   .select(field("Likes._id").as(BaseColumns._ID))
                                   .innerJoin(Tables.Sounds.TABLE, whereTrackDataExists)
                                   .whereEq(Tables.Likes._TYPE, Tables.Sounds.TYPE_TRACK)
                                   .order(Tables.Likes.CREATED_AT, DESC)
                                   .whereNull(Tables.Likes.REMOVED_AT))
                       .toList(new TrackUrnMapper());
    }
}
