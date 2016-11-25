package com.soundcloud.android.sync.stream;

import com.soundcloud.android.api.model.stream.ApiStreamItem;
import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.TxnResult;

import javax.inject.Inject;

class ReplaceSoundStreamCommand extends DefaultWriteStorageCommand<Iterable<ApiStreamItem>, TxnResult> {

    private final SoundStreamReplaceTransactionFactory soundStreamReplaceTransactionFactory;

    @Inject
    ReplaceSoundStreamCommand(PropellerDatabase propeller,
                              SoundStreamReplaceTransactionFactory factory) {
        super(propeller);
        soundStreamReplaceTransactionFactory = factory;
    }

    @Override
    protected TxnResult write(PropellerDatabase propeller, Iterable<ApiStreamItem> input) {
        return propeller.runTransaction(soundStreamReplaceTransactionFactory.create(input));
    }

}
