package com.soundcloud.android.storage;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.java.collections.Pair;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.subjects.ReplaySubject;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton // singleton so we can throttle reports
class SlowQueryReporter {

    @VisibleForTesting static final int LENGTH_TOLERANCE_MS = 3000;
    @VisibleForTesting static final int THROTTLE_TIME_MS = 5000;

    private static final String TAG = DebugQueryHook.TAG;
    private static final int MAX_RECENT_ITEMS = 30;

    private final ReplaySubject<DebugDatabaseStat> queryDurations = ReplaySubject.createWithSize(MAX_RECENT_ITEMS);

    @Inject
    SlowQueryReporter(DebugStorage debugStorage,
                      @Named(ApplicationModule.RX_LOW_PRIORITY) Scheduler scheduler) {
        this(debugStorage, scheduler, new DefaultSlowQueryObserver());
    }

    SlowQueryReporter(DebugStorage debugStorage, Scheduler scheduler, Observer<SlowQueryOutput> reporter) {
        queryDurations.filter(debugDatabaseStat -> debugDatabaseStat.duration() >= LENGTH_TOLERANCE_MS)
                      .observeOn(scheduler)
                      .throttleFirst(THROTTLE_TIME_MS, TimeUnit.MILLISECONDS)
                      .flatMapSingle(__ -> getTableStats(debugStorage).map(stats -> SlowQueryOutput.create(stats, Arrays.asList(queryDurations.getValues(new DebugDatabaseStat[]{})))))
                      .subscribe(reporter);
    }

    private Single<String> getTableStats(DebugStorage debugStorage) {
        return debugStorage.tableSizes()
                           .reduce(new StringBuilder(), this::appendTableStat)
                           .map(StringBuilder::toString);
    }

    @NonNull
    private StringBuilder appendTableStat(StringBuilder stringBuilder, Pair<String, Integer> stringIntegerPair) {
        return stringBuilder.append(stringIntegerPair.second()).append(" [")
                            .append(stringIntegerPair.first()).append("]")
                            .append(System.getProperty("line.separator"));
    }

    void reportIfSlow(DebugDatabaseStat report) {
        queryDurations.onNext(report);
    }

    private static class DefaultSlowQueryObserver extends DefaultObserver<SlowQueryOutput> {
        @Override
        public void onNext(SlowQueryOutput output) {
            for (DebugDatabaseStat stat : output.recentOperations()) {
                ErrorUtils.log(Log.DEBUG, TAG, "[" + stat.duration() + "ms] : " + DebugQueryHook.limit(stat.operation()));
            }
            ErrorUtils.log(Log.DEBUG, TAG, "Table Stats : " + output.tableStats());
            ErrorUtils.handleSilentException(new SQLRequestOverdueException());
        }
    }

    private static class SQLRequestOverdueException extends Exception {
    }

    @AutoValue
    static abstract class SlowQueryOutput {

        abstract List<DebugDatabaseStat> recentOperations();
        abstract String tableStats();

        static SlowQueryOutput create(String tableStats, List<DebugDatabaseStat> recentOperations) {
            return new AutoValue_SlowQueryReporter_SlowQueryOutput(recentOperations, tableStats);
        }
    }
}
