package com.soundcloud.android.sync.stream;

import com.soundcloud.android.api.model.stream.ApiStreamItem;
import com.soundcloud.android.commands.StoreCommand;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import javax.inject.Inject;

class StoreSoundStreamCommand extends StoreCommand<Iterable<ApiStreamItem>> {

    @Inject
    StoreSoundStreamCommand(PropellerDatabase database) {
        super(database);
    }

    @Override
    protected WriteResult store() {
        return database.runTransaction(new SoundStreamInsertTransaction(false, input));
    }
}
