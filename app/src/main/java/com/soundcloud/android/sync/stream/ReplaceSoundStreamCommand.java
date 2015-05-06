package com.soundcloud.android.sync.stream;

import com.soundcloud.android.api.model.stream.ApiStreamItem;
import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.TxnResult;

import javax.inject.Inject;

class ReplaceSoundStreamCommand extends DefaultWriteStorageCommand<Iterable<ApiStreamItem>, TxnResult> {

    @Inject
    ReplaceSoundStreamCommand(PropellerDatabase propeller) {
        super(propeller);
    }

    @Override
    protected TxnResult write(PropellerDatabase propeller, Iterable<ApiStreamItem> input) {
        return propeller.runTransaction(new SoundStreamReplaceTransaction(input));
    }

}
