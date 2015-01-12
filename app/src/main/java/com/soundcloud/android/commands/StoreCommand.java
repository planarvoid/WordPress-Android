package com.soundcloud.android.commands;

import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.PropellerWriteException;
import com.soundcloud.propeller.WriteResult;
import rx.Observable;

public abstract class StoreCommand<I> extends Command<I, WriteResult> {

    protected final PropellerDatabase database;

    protected StoreCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public WriteResult call() throws PropellerWriteException {
        final WriteResult result = store();
        if (result.success()) {
            return result;
        }
        throw result.getFailure();
    }

    protected abstract WriteResult store();

    @Override
    public Observable<WriteResult> toObservable(I input) {
        return super.toObservable(input).subscribeOn(ScSchedulers.STORAGE_SCHEDULER);
    }
}
