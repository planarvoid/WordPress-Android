package com.soundcloud.android.offline.commands;

import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.storage.Table;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import javax.inject.Inject;

public class ClearTrackDownloadsCommand extends DefaultWriteStorageCommand<Void, WriteResult> {

    @Inject
    ClearTrackDownloadsCommand(PropellerDatabase propeller) {
        super(propeller);
    }

    @Override
    protected WriteResult write(PropellerDatabase propeller, Void input) {
        return propeller.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                step(propeller.delete(Table.TrackDownloads));
                step(propeller.delete(Table.OfflineContent));
            }
        });
    }
}
