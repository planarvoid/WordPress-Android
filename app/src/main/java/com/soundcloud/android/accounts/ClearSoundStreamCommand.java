package com.soundcloud.android.accounts;

import com.soundcloud.android.commands.StoreCommand;
import com.soundcloud.android.storage.Table;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import javax.inject.Inject;


class ClearSoundStreamCommand extends StoreCommand<Void> {

    @Inject
    public ClearSoundStreamCommand(PropellerDatabase database) {
        super(database);
    }

    @Override
    protected WriteResult store() {
        return database.delete(Table.SoundStream);
    }
}
