package com.soundcloud.android.sync.likes;

import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.PropellerDatabase;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class RemoveLikesCommand extends DefaultWriteStorageCommand<Collection<LikeRecord>, ChangeResult> {

    private final int type;

    @Inject
    RemoveLikesCommand(PropellerDatabase database, int type) {
        super(database);
        this.type = type;
    }

    @Override
    protected ChangeResult write(PropellerDatabase propeller, Collection<LikeRecord> input) {
        List<Long> ids = new ArrayList<>(input.size());
        for (LikeRecord like : input) {
            ids.add(like.getTargetUrn().getNumericId());
        }
        return propeller.delete(Tables.Likes.TABLE, filter()
                .whereIn(Tables.Likes._ID, ids)
                .whereEq(Tables.Likes._TYPE, type));
    }
}
