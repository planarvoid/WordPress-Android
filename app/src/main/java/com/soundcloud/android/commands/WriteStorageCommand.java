package com.soundcloud.android.commands;

import com.google.common.base.Preconditions;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import android.os.Looper;

import javax.inject.Provider;

public abstract class WriteStorageCommand<I, R extends WriteResult, O> extends Command<I, O> {

    public static final Provider<Thread> CURRENT_THREAD_PROVIDER = new Provider<Thread>() {
        @Override
        public Thread get() {
            return Thread.currentThread();
        }
    };

    private final PropellerDatabase propeller;
    private final Provider<Thread> currentThreadProvider;

    protected WriteStorageCommand(PropellerDatabase propeller) {
        this(propeller, CURRENT_THREAD_PROVIDER);
    }

    protected WriteStorageCommand(PropellerDatabase propeller, Provider<Thread> currentThreadProvider) {
        this.propeller = propeller;
        this.currentThreadProvider = currentThreadProvider;
    }

    @Override
    public O call(I input) {
        assertBackgroundThread();
        final R result = write(propeller, input);
        if (result.success()) {
            return transform(result);
        }
        throw result.getFailure();
    }

    private void assertBackgroundThread() {
        Preconditions.checkState(
                currentThreadProvider.get() != Looper.getMainLooper().getThread(),
                "Attempting to run a write command on the main thread");
    }

    protected abstract R write(PropellerDatabase propeller, I input);

    protected abstract O transform(R result);
}
