package com.soundcloud.android.storage;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.java.collections.Pair;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;

@Singleton // singleton so we can throttle reports
class SlowQueryReporter {

    @VisibleForTesting static final int LENGTH_TOLERANCE_MS = 3000;
    @VisibleForTesting static final int THROTTLE_TIME_MS = 5000;

    private static final String TAG = DebugQueryHook.TAG;

    private final PublishSubject<Long> queryDurations = PublishSubject.create();

    @Inject
    SlowQueryReporter(DebugStorage debugStorage,
                      @Named(ApplicationModule.RX_LOW_PRIORITY) Scheduler scheduler) {
        this(debugStorage, scheduler, new DefaultSlowQueryObserver());
    }

    SlowQueryReporter(DebugStorage debugStorage, Scheduler scheduler, Observer<String> reporter) {
        queryDurations.filter(aLong -> aLong >= LENGTH_TOLERANCE_MS)
                      .observeOn(scheduler)
                      .throttleFirst(THROTTLE_TIME_MS, TimeUnit.MILLISECONDS)
                      .flatMapSingle(__ -> getTableStats(debugStorage))
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

    void reportIfSlow(long duration) {
        queryDurations.onNext(duration);
    }

    private static class DefaultSlowQueryObserver extends DefaultObserver<String> {
        @Override
        public void onNext(String stats) {
            ErrorUtils.log(Log.DEBUG, TAG, "Table Stats : " + stats);
            ErrorUtils.handleSilentException(new SQLRequestOverdueException());
        }
    }

    private static class SQLRequestOverdueException extends Exception {
    }
}
