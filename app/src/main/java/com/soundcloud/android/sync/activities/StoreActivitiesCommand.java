package com.soundcloud.android.sync.activities;

import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.TxnResult;

import javax.inject.Inject;

class StoreActivitiesCommand extends DefaultWriteStorageCommand<Iterable<ApiActivityItem>, TxnResult> {

    @Inject
    StoreActivitiesCommand(PropellerDatabase propeller) {
        super(propeller);
    }

    @Override
    protected TxnResult write(PropellerDatabase propeller, Iterable<ApiActivityItem> input) {
        return new TxnResult();
    }
}
