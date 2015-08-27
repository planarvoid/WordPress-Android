package com.soundcloud.android;

import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.MemoryReporter;
import rx.Observable;
import rx.Scheduler;

import android.support.annotation.NonNull;

import java.util.concurrent.TimeUnit;

// Note : don't use injection in this class.
// It is used before Dagger is setup - otherwise we would never report potential Dagger crashes.
class UncaughtExceptionHandlerController {

    private final Scheduler scheduler;
    private final MemoryReporter memoryReporter;

    UncaughtExceptionHandlerController(Scheduler scheduler, MemoryReporter memoryReporter) {
        this.scheduler = scheduler;
        this.memoryReporter = memoryReporter;
    }

    void reportSystemMemoryStats() {
        memoryReporter.reportSystemMemoryStats();
    }

    void reportMemoryTrim(int level) {
        memoryReporter.reportMemoryTrim(level);
    }

    void setupUncaughtExceptionHandler() {
        watch(ErrorUtils.setupUncaughtExceptionHandler(memoryReporter));
    }

    private void watch(Thread.UncaughtExceptionHandler handler) {
        Observable
                .timer(1, TimeUnit.MINUTES, scheduler)
                .subscribe(new WatchHandlerSubscriber(handler));
    }

    private class WatchHandlerSubscriber extends DefaultSubscriber<Object> {
        private final Thread.UncaughtExceptionHandler expectedHandler;

        public WatchHandlerSubscriber(Thread.UncaughtExceptionHandler expectedHandler) {
            this.expectedHandler = expectedHandler;
        }

        @Override
        public void onNext(Object ignored) {
            if (expectedHandler != Thread.getDefaultUncaughtExceptionHandler()) {
                final String detailMessage = createErrorMessage();
                setupUncaughtExceptionHandler();
                ErrorUtils.handleSilentException(detailMessage, new IllegalUncaughtExceptionHandlerException(detailMessage));
            }
        }

        @NonNull
        private String createErrorMessage() {
            return "Illegal handler: " + Thread.getDefaultUncaughtExceptionHandler();
        }
    }

    class IllegalUncaughtExceptionHandlerException extends RuntimeException {
        public IllegalUncaughtExceptionHandlerException(String detailMessage) {
            super(detailMessage);
        }
    }
}
