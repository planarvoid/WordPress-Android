package com.soundcloud.android.commands;

import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.rx.RxResultMapper;
import rx.Observable;

import java.util.List;

public abstract class PagedQueryCommand<I extends PagedQueryCommand.PageParams> extends LegacyCommand<I, List<PropertySet>, PagedQueryCommand<I>> {

    private final PropellerDatabase database;
    private final RxResultMapper<PropertySet> mapper;

    public PagedQueryCommand(PropellerDatabase database, RxResultMapper<PropertySet> mapper) {
        this.mapper = mapper;
        this.database = database;
    }

    protected abstract Query buildQuery(I input);

    private Query buildPagedQuery(I input){
        final Query query = buildQuery(input);
        if (input != null){
            return query.limit(input.getLimit());
        } else {
            return query;
        }
    }

    @Override
    public Observable<List<PropertySet>> toObservable() {
        return super.toObservable().subscribeOn(ScSchedulers.HIGH_PRIO_SCHEDULER);
    }

    @Override
    public List<PropertySet> call() throws Exception {
        return database.query(buildPagedQuery(input)).toList(mapper);
    }

    public static class PageParams {
        private final int limit;

        protected PageParams(int limit) {
            this.limit = limit;
        }

        public int getLimit() {
            return limit;
        }
    }
}


