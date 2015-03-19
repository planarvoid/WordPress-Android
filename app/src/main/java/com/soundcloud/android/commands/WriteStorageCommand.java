package com.soundcloud.android.commands;

import com.google.common.base.Preconditions;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import android.os.Looper;

import javax.inject.Provider;

public abstract class WriteStorageCommand<I, R extends WriteResult, O> extends Command<I, O> {

    private final PropellerDatabase propeller;
    private final Provider<Thread> currentThreadProvider;

    protected WriteStorageCommand(PropellerDatabase propeller) {
        this(propeller, new Provider<Thread>() {
            @Override
            public Thread get() {
                return Looper.getMainLooper().getThread();
            }
        });
    }

    protected WriteStorageCommand(PropellerDatabase propeller, Provider<Thread> currentThreadProvider) {
        this.propeller = propeller;
        this.currentThreadProvider = currentThreadProvider;
    }

    @Override
    public O call(I input) {
        Preconditions.checkState(currentThreadProvider.get() != Looper.getMainLooper().getThread());
        final R result = write(propeller, input);
        if (result.success()) {
            return transform(result);
        }
        throw result.getFailure();
    }

    protected abstract R write(PropellerDatabase propeller, I input);

    protected abstract O transform(R result);
}
