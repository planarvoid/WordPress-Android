package com.soundcloud.android.sync.likes;

import com.soundcloud.android.commands.StoreCommand;
import com.soundcloud.android.storage.Table;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import javax.inject.Inject;

public class RemoveAllLikesCommand extends StoreCommand<Void> {

    @Inject
    RemoveAllLikesCommand(PropellerDatabase database) {
        super(database);
    }

    @Override
    protected WriteResult store() {
        return database.delete(Table.Likes);
    }
}
