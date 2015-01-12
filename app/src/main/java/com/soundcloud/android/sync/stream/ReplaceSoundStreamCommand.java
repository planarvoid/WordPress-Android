package com.soundcloud.android.sync.stream;

import com.soundcloud.android.api.model.stream.ApiStreamItem;
import com.soundcloud.android.commands.StoreCommand;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import javax.inject.Inject;

class ReplaceSoundStreamCommand extends StoreCommand<Iterable<ApiStreamItem>> {

    @Inject
    ReplaceSoundStreamCommand(PropellerDatabase database) {
        super(database);
    }

    @Override
    protected WriteResult store() {
        return database.runTransaction(new SoundStreamInsertTransaction(true, input));
    }

}
