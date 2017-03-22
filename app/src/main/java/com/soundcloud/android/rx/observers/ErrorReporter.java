package com.soundcloud.android.rx.observers;

import static com.soundcloud.android.rx.OperationDurationLogger.report;

import com.soundcloud.android.BuildConfig;
import com.soundcloud.android.rx.OperationDurationLogger;
import com.soundcloud.android.utils.CallsiteToken;
import com.soundcloud.android.utils.ErrorUtils;

import java.util.concurrent.TimeUnit;

/**
 * <p>Helper for error reporting</p>
 * (cf. {@link com.soundcloud.android.utils.ErrorUtils#handleThrowable(Throwable, com.soundcloud.android.utils.CallsiteToken)}
 */
class ErrorReporter {
    private final CallsiteToken callsiteToken = CallsiteToken.build();
    private final OperationDurationLogger.TimeMeasure measure =
            OperationDurationLogger.create(callsiteToken.getStackTrace(), BuildConfig.DEBUG);

    ErrorReporter() {}

    void handleOnStart() {
        measure.start();
    }

    void handleOnComplete() {
        reportDuration();
    }

    void handleOnError(Throwable throwable) {
        reportDuration();
        ErrorUtils.handleThrowable(throwable, callsiteToken);
    }

    private void reportDuration() {
        measure.stop();
        report(measure, 2, TimeUnit.SECONDS);
    }
}
