package com.soundcloud.android.sync.likes;

import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.StoreCommand;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class RemoveLikesCommand extends StoreCommand<Collection<PropertySet>> {

    private final int type;

    @Inject
    RemoveLikesCommand(PropellerDatabase database, int type) {
        super(database);
        this.type = type;
    }

    @Override
    protected WriteResult store() {
        List<Long> ids = new ArrayList<>(input.size());
        for (PropertySet like : input) {
            ids.add(like.get(LikeProperty.TARGET_URN).getNumericId());
        }
        return database.delete(Table.Likes, filter()
                .whereIn(TableColumns.Likes._ID, ids)
                .whereEq(TableColumns.Likes._TYPE, type));
    }
}
