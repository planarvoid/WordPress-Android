package com.soundcloud.android.commands;

import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import javax.inject.Provider;

public abstract class DefaultWriteStorageCommand<I, O extends WriteResult> extends WriteStorageCommand<I, O, O> {
    protected DefaultWriteStorageCommand(PropellerDatabase propeller) {
        super(propeller);
    }

    protected DefaultWriteStorageCommand(PropellerDatabase propeller, Provider<Thread> currentThreadProvider) {
        super(propeller, currentThreadProvider);
    }

    @Override
    protected O transform(O result) {
        return result;
    }
}
