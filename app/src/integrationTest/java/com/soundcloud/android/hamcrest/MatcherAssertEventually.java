package com.soundcloud.android.hamcrest;

import static com.soundcloud.android.hamcrest.MatchersEventually.retry;
import static org.hamcrest.MatcherAssert.assertThat;

import com.soundcloud.android.playlists.NewPlaylistDetailsPresenterIntegrationTest;
import com.soundcloud.android.utils.Sleeper;
import com.soundcloud.android.utils.Supplier;
import org.hamcrest.Matcher;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;


// Credits : https://pilchardfriendly.wordpress.com/2009/01/31/using-hamcrest-to-assert-asynchronous-behaviour/
public class MatcherAssertEventually {

    public static <T> void assertThatEventually(Supplier<T> supplier, Matcher<T> matcher) {
        assertThat(networkCondition(supplier), retry(matcher));
    }

    private static <T> Iterable<T> networkCondition(Supplier<T> supplier) {
        return within(supplier, 2, TimeUnit.SECONDS.toMillis(2));
    }

    static <T> Iterable<T> within(Supplier<T> supplier, long durationUntilRetry, long timeout) {
        final Sleeper sleeper = new Sleeper();
        final StopWatch stopWatch = new StopWatch(timeout);

        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    private boolean isFirstTime = true;

                    @Override
                    public boolean hasNext() {
                        if (isFirstTime) {
                            isFirstTime = false;
                            stopWatch.startCountDown();
                        }
                        return !stopWatch.isTimeElapsed();
                    }

                    @Override
                    public T next() {
                        // Also needs to wait for the initial call
                        sleeper.sleep(durationUntilRetry);
                        return poll();
                    }

                    private T poll() {
                        log("Polling. Time remaining " + stopWatch.countDown() + " ms");
                        return supplier.get();
                    }

                    private void log(String message) {
                        System.out.println(message);
                    }

                };
            }
        };
    }

    private static class StopWatch {

        private final long duration;
        private long startTime;

        StopWatch(long duration) {
            this.duration = duration;
        }

        void startCountDown() {
            startTime = currentTime();
        }

        boolean isTimeElapsed() {
            return countDown() < 0;
        }

        long countDown() {
            return duration - elapsedTime();
        }

        private long elapsedTime() {
            return currentTime() - startTime;
        }

        private static long currentTime() {
            return System.currentTimeMillis();
        }
    }

}
