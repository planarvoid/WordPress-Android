package com.soundcloud.android.utils;

import static com.soundcloud.java.checks.Preconditions.checkArgument;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class TryWithBackOff<T> {

    private final Sleeper sleeper;
    private final long initialBackOffTime;
    private final TimeUnit timeUnit;
    private final int backOffMultiplier;
    private final int maxAttempts;

    TryWithBackOff(Sleeper sleeper,
                   long initialBackOffTime,
                   TimeUnit timeUnit,
                   int backOffMultiplier,
                   int maxAttempts) {
        checkArgument(maxAttempts > 0);
        this.sleeper = sleeper;
        this.initialBackOffTime = initialBackOffTime;
        this.timeUnit = timeUnit;
        this.backOffMultiplier = backOffMultiplier;
        this.maxAttempts = maxAttempts;
    }

    public T call(Callable<T> callable) throws Exception {
        long backOffTime = initialBackOffTime;
        Exception lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return callable.call();
            } catch (Exception e) {
                e.printStackTrace();
                lastFailure = e;
                sleeper.sleep(backOffTime, timeUnit);
                backOffTime *= backOffMultiplier;
            }
        }
        throw lastFailure;
    }

    @Singleton
    public static class Factory {

        private static final int INITIAL_BACK_OFF_TIME = 1;
        private static final int BACK_OFF_MULTIPLIER = 2;
        private static final int MAX_ATTEMPTS = 3;

        private final Sleeper sleeper;

        @Inject
        public Factory(Sleeper sleeper) {
            this.sleeper = sleeper;
        }

        public <T> TryWithBackOff<T> withDefaults() {
            return create(INITIAL_BACK_OFF_TIME, TimeUnit.SECONDS, BACK_OFF_MULTIPLIER, MAX_ATTEMPTS);
        }

        public <T> TryWithBackOff<T> create(long initialBackOffTime,
                                            TimeUnit timeUnit,
                                            int backOffMultiplier,
                                            int maxAttempts) {
            return new TryWithBackOff<>(sleeper, initialBackOffTime, timeUnit, backOffMultiplier, maxAttempts);
        }
    }
}
