package com.soundcloud.android.commands;

import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

public abstract class DefaultWriteStorageCommand<I, O extends WriteResult> extends WriteStorageCommand<I, O, O> {
    protected DefaultWriteStorageCommand(PropellerDatabase propeller) {
        super(propeller);
    }

    @Override
    protected O transform(O result) {
        return result;
    }
}
