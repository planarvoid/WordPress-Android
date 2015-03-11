package com.soundcloud.android.commands;

import com.soundcloud.propeller.WriteResult;

public abstract class WriteStorageCommandNG<I, O extends WriteResult> extends CommandNG<I, O> {

    @Override
    public O call(I input) {
        final O result = store(input);
        if (result.success()) {
            return result;
        }
        throw result.getFailure();
    }

    protected abstract O store(I input);
}
