package com.soundcloud.android.offline.commands;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.android.storage.Table;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import javax.inject.Inject;
import javax.inject.Provider;

public class ClearTrackDownloadsCommand extends DefaultWriteStorageCommand<Void, WriteResult> {

    @Inject
    ClearTrackDownloadsCommand(PropellerDatabase propeller) {
        super(propeller);
    }

    @VisibleForTesting
    ClearTrackDownloadsCommand(PropellerDatabase propeller, Provider<Thread> currentThreadProvider) {
        super(propeller, currentThreadProvider);
    }

    @Override
    protected WriteResult write(PropellerDatabase propeller, Void input) {
        return propeller.delete(Table.TrackDownloads);
    }
}
