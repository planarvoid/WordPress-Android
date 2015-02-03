package com.soundcloud.android.likes;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.commands.UrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;

import javax.inject.Inject;
import java.util.List;

public class LoadLikedTrackUrnsCommand extends Command<Object, List<Urn>, LoadLikedTrackUrnsCommand> {

    private final PropellerDatabase database;

    @Inject
    LoadLikedTrackUrnsCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public List<Urn> call() throws Exception {
        return database.query(Query.from(Table.Likes.name())
                .whereEq(TableColumns.Likes._TYPE, TableColumns.Sounds.TYPE_TRACK)
                .order(TableColumns.Likes.CREATED_AT, Query.ORDER_DESC)
                .whereNull(TableColumns.Likes.REMOVED_AT))
                .toList(new UrnMapper());
    }
}
