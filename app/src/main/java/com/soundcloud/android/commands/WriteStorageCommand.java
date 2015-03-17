package com.soundcloud.android.commands;

import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

public abstract class WriteStorageCommand<I, O extends WriteResult> extends Command<I, O> {

    private final PropellerDatabase propeller;

    protected WriteStorageCommand(PropellerDatabase propeller) {
        this.propeller = propeller;
    }

    @Override
    public O call(I input) {
        final O result = write(propeller, input);
        if (result.success()) {
            return result;
        }
        throw result.getFailure();
    }

    protected abstract O write(PropellerDatabase propeller, I input);
}
