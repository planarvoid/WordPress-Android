package com.soundcloud.android.sync.likes;

import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.PropellerDatabase;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class RemoveLikesCommand extends DefaultWriteStorageCommand<Collection<PropertySet>, ChangeResult> {

    private final int type;

    @Inject
    RemoveLikesCommand(PropellerDatabase database, int type) {
        super(database);
        this.type = type;
    }

    @Override
    protected ChangeResult write(PropellerDatabase propeller, Collection<PropertySet> input) {
        List<Long> ids = new ArrayList<>(input.size());
        for (PropertySet like : input) {
            ids.add(like.get(LikeProperty.TARGET_URN).getNumericId());
        }
        return propeller.delete(Tables.Likes.TABLE, filter()
                .whereIn(Tables.Likes._ID, ids)
                .whereEq(Tables.Likes._TYPE, type));
    }
}
