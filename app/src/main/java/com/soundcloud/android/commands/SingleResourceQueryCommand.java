package com.soundcloud.android.commands;

import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.DatabaseScheduler;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;

import java.util.List;

@Deprecated
public abstract class SingleResourceQueryCommand<I> extends LegacyCommand<I, PropertySet, SingleResourceQueryCommand<I>> {

    private final PropellerDatabase database;
    private final DatabaseScheduler databaseScheduler;
    private final RxResultMapper<PropertySet> mapper;

    public SingleResourceQueryCommand(DatabaseScheduler databaseScheduler, RxResultMapper<PropertySet> mapper) {
        this.mapper = mapper;
        this.database = databaseScheduler.database();
        this.databaseScheduler = databaseScheduler;
    }

    protected abstract Query buildQuery(I input);

    @Override
    public Observable<PropertySet> toObservable() {
        return databaseScheduler.scheduleQuery(buildQuery(input)).map(mapper);
    }

    @Override
    public PropertySet call() throws Exception {
        final List<PropertySet> queryResult = database.query(buildQuery(input)).toList(mapper);
        return queryResult.isEmpty() ? PropertySet.create() : queryResult.get(0);
    }
}
