package com.soundcloud.android.sync.activities;

import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.comments.StoreCommentCommand;
import com.soundcloud.android.storage.Table;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.TxnResult;

import javax.inject.Inject;

class ReplaceActivitiesCommand extends StoreActivitiesCommand {

    @Inject
    ReplaceActivitiesCommand(PropellerDatabase propeller,
                             StoreUsersCommand storeUsersCommand,
                             StoreTracksCommand storeTracksCommand,
                             StorePlaylistsCommand storePlaylistsCommand,
                             StoreCommentCommand storeCommentCommand) {
        super(propeller, storeUsersCommand, storeTracksCommand, storePlaylistsCommand, storeCommentCommand);
    }

    @Override
    protected TxnResult write(PropellerDatabase propeller, final Iterable<ApiActivityItem> input) {
        return propeller.runTransaction(new ReplaceActivitiesTransaction(input));
    }

    private class ReplaceActivitiesTransaction extends StoreActivitiesTransaction {

        ReplaceActivitiesTransaction(Iterable<ApiActivityItem> activities) {
            super(activities);
        }

        @Override
        public void steps(PropellerDatabase propeller) {
            step(propeller.delete(Table.Activities));
            super.steps(propeller);
        }
    }
}