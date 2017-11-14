package com.soundcloud.android.sync.stream;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.api.model.stream.ApiStreamItem;
import com.soundcloud.android.commands.StorePlaylistsCommand;
import com.soundcloud.android.commands.StoreUsersCommand;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.tracks.TrackStorage;
import com.soundcloud.propeller.PropellerDatabase;

@AutoFactory(allowSubclasses = true)
public class SoundStreamReplaceTransaction extends SoundStreamInsertTransaction {

    SoundStreamReplaceTransaction(Iterable<ApiStreamItem> streamItems,
                                  @Provided StoreUsersCommand storeUsersCommand,
                                  @Provided TrackStorage trackStorage,
                                  @Provided StorePlaylistsCommand storePlaylistsCommand) {
        super(streamItems, storeUsersCommand, trackStorage, storePlaylistsCommand);
    }

    @Override
    protected void beforeInserts(PropellerDatabase propeller) {
        super.beforeInserts(propeller);
        step(propeller.delete(Table.SoundStream));
    }
}
