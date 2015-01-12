package com.soundcloud.android.sync.commands;

import com.soundcloud.android.api.model.stream.ApiStreamItem;
import com.soundcloud.android.commands.StoreCommand;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import javax.inject.Inject;

public class ReplaceSoundStreamCommand extends StoreCommand<Iterable<ApiStreamItem>> {

    @Inject
    public ReplaceSoundStreamCommand(PropellerDatabase database) {
        super(database);
    }

    @Override
    protected WriteResult store() {
        return database.runTransaction(new SoundStreamInsertTransaction(true, input));
    }

}
