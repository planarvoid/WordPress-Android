package com.soundcloud.android.sync.likes;

import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.PropellerDatabase;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class RemoveLikesCommand extends DefaultWriteStorageCommand<Collection<LikeRecord>, ChangeResult> {

    private static final int BATCH_SIZE = 500;
    private final int type;

    @Inject
    RemoveLikesCommand(PropellerDatabase database, int type) {
        super(database);
        this.type = type;
    }

    @Override
    protected ChangeResult write(PropellerDatabase propeller, Collection<LikeRecord> input) {
        for (List<Long> batch : Lists.partition(Lists.transform(new ArrayList<>(input), input1 -> input1.getTargetUrn().getNumericId()), BATCH_SIZE)) {
            ChangeResult delete = propeller.delete(Tables.Likes.TABLE, filter()
                    .whereIn(Tables.Likes._ID, batch)
                    .whereEq(Tables.Likes._TYPE, type));
            if (!delete.success()) {
                return delete;
            }
        }
        return new ChangeResult(input.size());
    }
}
