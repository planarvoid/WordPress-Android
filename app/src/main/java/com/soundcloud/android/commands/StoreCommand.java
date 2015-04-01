package com.soundcloud.android.commands;

import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;
import rx.Observable;

@Deprecated // use WriteStorageCommand
public abstract class StoreCommand<I> extends LegacyCommand<I, WriteResult, StoreCommand<I>> {

    protected final PropellerDatabase database;

    protected StoreCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public WriteResult call() {
        final WriteResult result = store();
        if (result.success()) {
            return result;
        }
        throw result.getFailure();
    }

    protected abstract WriteResult store();

    @Override
    public Observable<WriteResult> toObservable() {
        return super.toObservable().subscribeOn(ScSchedulers.HIGH_PRIO_SCHEDULER);
    }
}
