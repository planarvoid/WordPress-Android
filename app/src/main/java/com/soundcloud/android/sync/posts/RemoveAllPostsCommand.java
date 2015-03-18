package com.soundcloud.android.sync.posts;

import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.storage.Table;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import javax.inject.Inject;

public class RemoveAllPostsCommand extends DefaultWriteStorageCommand<Void, WriteResult> {

    @Inject
    RemoveAllPostsCommand(PropellerDatabase propeller) {
        super(propeller);
    }

    @Override
    protected WriteResult write(PropellerDatabase propeller, Void input) {
        return propeller.delete(Table.Posts);
    }
}
