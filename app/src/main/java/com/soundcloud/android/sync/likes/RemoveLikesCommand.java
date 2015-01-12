package com.soundcloud.android.sync.likes;

import com.soundcloud.android.commands.StoreCommand;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.query.WhereBuilder;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class RemoveLikesCommand extends StoreCommand<Collection<PropertySet>> {

    @Inject
    RemoveLikesCommand(PropellerDatabase database) {
        super(database);
    }

    @Override
    protected WriteResult store() {
        List<Long> ids = new ArrayList<>(input.size());
        for (PropertySet like : input) {
            ids.add(like.get(LikeProperty.TARGET_URN).getNumericId());
        }
        return database.delete(Table.Likes, new WhereBuilder().whereIn(TableColumns.Likes._ID, ids));
    }
}
