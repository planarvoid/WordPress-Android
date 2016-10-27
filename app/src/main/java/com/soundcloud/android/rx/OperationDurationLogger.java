package com.soundcloud.android.rx;

import static com.soundcloud.java.checks.Preconditions.checkArgument;
import static com.soundcloud.java.checks.Preconditions.checkState;
import static java.util.Arrays.asList;

import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.java.strings.Strings;

import android.util.Log;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class OperationDurationLogger {

    public static TimeMeasure create(StackTraceElement[] stackTrace, boolean isLoggingEnabled) {
        if (isLoggingEnabled) {
            return create(stackTrace);
        } else {
            return OperationDurationLogger.empty();
        }
    }

    public static TimeMeasure create(StackTraceElement[] stackTrace) {
        return create(stackTrace, new CurrentDateProvider());
    }

    public static TimeMeasure create(StackTraceElement[] stackTrace, DateProvider dateProvider) {
        final String measureName = findSubscriptionSource(stackTrace);
        return TimeMeasure.from(measureName, dateProvider);
    }

    public static TimeMeasure empty() {
        return TimeMeasure.EMPTY;
    }

    private static String findSubscriptionSource(StackTraceElement[] stackTrace) {
        checkArgument(stackTrace.length > 0, "The stack trace can't be empty.");

        final String source = findSubscriptionSource(asList(stackTrace).iterator());
        if (source.isEmpty()) {
            return stackTrace[0].toString();
        } else {
            return source;
        }
    }

    static private String findSubscriptionSource(Iterator<StackTraceElement> stackTraceElementIterator) {
        if (!stackTraceElementIterator.hasNext()) {
            return Strings.EMPTY;
        }

        final String traceElement = stackTraceElementIterator.next().toString();
        if (isAppSunscriptionElement(traceElement)) {
            return traceElement;
        }

        return findSubscriptionSource(stackTraceElementIterator);
    }

    static public void report(TimeMeasure measure, int threshold, TimeUnit timeUnit) {
        if (measure == TimeMeasure.EMPTY) {
            return;
        }

        final long duration = measure.duration(TimeUnit.MILLISECONDS);
        final String message = "Operation took " + duration + " ms. Subscribed from " + measure.name();
        if (duration > timeUnit.toMillis(threshold)) {
            ErrorUtils.log(Log.WARN, OperationsInstrumentation.TAG, message);
        } else {
            ErrorUtils.log(Log.DEBUG, OperationsInstrumentation.TAG, message);
        }
    }

    static private boolean isAppSunscriptionElement(String traceElement) {
        return !traceElement.startsWith("com.soundcloud.android.rx")
                && traceElement.startsWith("com.soundcloud.android.");
    }

    public abstract static class TimeMeasure {
        public static final int UNKNOWN = -1;
        private final String name;

        private TimeMeasure(final String name) {
            this.name = name;
        }

        public static final TimeMeasure EMPTY = new TimeMeasure("Empty") {
            @Override
            public void start() {
                // no-op
            }

            @Override
            public void stop() {
                // no-op
            }

            @Override
            public long duration(TimeUnit unit) {
                return UNKNOWN;
            }
        };

        static TimeMeasure from(final String name, final DateProvider dateProvider) {
            return new TimeMeasure(name) {
                private long startTime = UNKNOWN;
                private long stopTime = UNKNOWN;

                @Override
                public void start() {
                    checkState(startTime == UNKNOWN, "Cannot start a measure if already stated.");
                    startTime = dateProvider.getCurrentTime();
                }

                @Override
                public void stop() {
                    checkState(stopTime == UNKNOWN, "Cannot stop a measure if already stopped.");
                    stopTime = dateProvider.getCurrentTime();
                }

                @Override
                public long duration(TimeUnit unit) {
                    if (startTime == UNKNOWN) {
                        return UNKNOWN;
                    }
                    if (stopTime == UNKNOWN) {
                        return convert(unit, dateProvider.getCurrentTime() - startTime);
                    }
                    return convert(unit, stopTime - startTime);
                }
            };
        }


        public String name() {
            return name;
        }

        public abstract void start();

        public abstract void stop();

        public abstract long duration(TimeUnit unit);

        static long convert(TimeUnit unit, long duration) {
            return unit.convert(duration, TimeUnit.MILLISECONDS);
        }
    }

}
