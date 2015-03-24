package com.soundcloud.android.commands;

import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

public abstract class WriteStorageCommand<I, R extends WriteResult, O> extends Command<I, O> {

    private final PropellerDatabase propeller;

    protected WriteStorageCommand(PropellerDatabase propeller) {
        this.propeller = propeller;
    }

    @Override
    public O call(I input) {
        final R result = write(propeller, input);
        if (result.success()) {
            return transform(result);
        }
        throw result.getFailure();
    }

    protected abstract R write(PropellerDatabase propeller, I input);

    protected abstract O transform(R result);
}
