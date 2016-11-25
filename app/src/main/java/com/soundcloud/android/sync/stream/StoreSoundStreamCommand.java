package com.soundcloud.android.sync.stream;

import com.soundcloud.android.api.model.stream.ApiStreamItem;
import com.soundcloud.android.commands.DefaultWriteStorageCommand;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.TxnResult;

import javax.inject.Inject;

class StoreSoundStreamCommand extends DefaultWriteStorageCommand<Iterable<ApiStreamItem>, TxnResult> {

    private final SoundStreamInsertTransactionFactory transactionFactory;

    @Inject
    StoreSoundStreamCommand(PropellerDatabase propeller, SoundStreamInsertTransactionFactory transactionFactory) {
        super(propeller);
        this.transactionFactory = transactionFactory;
    }

    @Override
    protected TxnResult write(PropellerDatabase propeller, Iterable<ApiStreamItem> input) {
        return propeller.runTransaction(transactionFactory.create(input));
    }

}
