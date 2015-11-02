package com.soundcloud.android.sync.activities;

import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.comments.StoreCommentCommand;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.TxnResult;

import javax.inject.Inject;

//TODO
class ReplaceActivitiesCommand extends DefaultWriteStorageCommand<Iterable<ApiActivityItem>, TxnResult> {

    @Inject
    ReplaceActivitiesCommand(PropellerDatabase propeller) {
        super(propeller);
    }

    @Override
    protected TxnResult write(PropellerDatabase propeller, Iterable<ApiActivityItem> input) {
        return new StoreActivitiesCommand(propeller, new StoreCommentCommand(propeller)).call(input);
    }
}
