package com.soundcloud.android.rx;

import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.NonFatalRuntimeException;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.TimeInterval;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class OperationsInstrumentation {

    public static <T> Observable.Transformer<T, T> reportOverdue() {
        final TimeUnit timeUnit = TimeUnit.SECONDS;
        final int threshold = 5;
        final UnresponsiveAppException callSite = new UnresponsiveAppException();

        return reportOverdue(threshold, timeUnit, new Action1<Long>() {
            @Override
            public void call(Long duration) {
                final String message = String.format(Locale.US, "Operation took too long : %d ms (expected < %d)", duration, timeUnit.toMillis(threshold));
                ErrorUtils.handleSilentException(message, callSite);
            }
        });
    }

    public static <T> Observable.Transformer<T, T> reportOverdue(long delay, TimeUnit unit,
                                                                 Action1<Long> reportAction) {
        return new ReportOverdueTransformer<>(unit.toMillis(delay), reportAction);
    }

    public static class UnresponsiveAppException extends NonFatalRuntimeException {
    }

    public static class ReportOverdueTransformer<T> implements Observable.Transformer<T, T> {

        private final Func1<TimeInterval<T>, T> unwrap = new Func1<TimeInterval<T>, T>() {
            @Override
            public T call(TimeInterval<T> timeInterval) {
                return timeInterval.getValue();
            }
        };
        private final Action1<Long> reportAction;
        private final long maxDuration;

        public ReportOverdueTransformer(long maxDuration, Action1<Long> reportAction) {
            this.reportAction = reportAction;
            this.maxDuration = maxDuration;
        }

        @Override
        public Observable<T> call(Observable<T> operation) {
            return operation
                    .timeInterval()
                    .doOnNext(reportOverdue(maxDuration, reportAction))
                    .map(unwrap);
        }

        private Action1<TimeInterval<T>> reportOverdue(final long maxDuration, final Action1<Long> reportAction) {
            return new Action1<TimeInterval<T>>() {
                @Override
                public void call(TimeInterval<T> timeInterval) {
                    final long duration = timeInterval.getIntervalInMilliseconds();
                    if (duration >= maxDuration) {
                        reportAction.call(duration);
                    }
                }
            };
        }
    }
}
