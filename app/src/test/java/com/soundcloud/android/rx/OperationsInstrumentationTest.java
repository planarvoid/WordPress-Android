package com.soundcloud.android.rx;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import rx.Observable;
import rx.functions.Action1;
import rx.observers.TestSubscriber;
import rx.schedulers.TestScheduler;

import java.util.concurrent.TimeUnit;

public class OperationsInstrumentationTest {

    private final TestScheduler scheduler = new TestScheduler();
    private final TestSubscriber<Object> subscriber = new TestSubscriber<>();
    private final SubbedReportAction reportAction = new SubbedReportAction();
    private final TimeUnit timeUnit = TimeUnit.SECONDS;

    @Test
    public void overdueReport() {
        Observable
                .timer(5, timeUnit, scheduler).first()
                .compose(OperationsInstrumentation.reportOverdue(0, timeUnit, reportAction))
                .subscribe(subscriber);

        scheduler.advanceTimeBy(3, timeUnit);
        assertThat(reportAction.hadTimeout).isFalse();
        subscriber.assertNoValues();

        scheduler.advanceTimeBy(3, timeUnit);
        subscriber.assertCompleted();
        assertThat(reportAction.hadTimeout).isTrue();
    }

    @Test
    public void overdueSilent() {
        Observable
                .timer(5, timeUnit, scheduler).first()
                .compose(OperationsInstrumentation.reportOverdue(6, timeUnit, reportAction))
                .subscribe(subscriber);

        scheduler.advanceTimeBy(3, timeUnit);
        assertThat(reportAction.hadTimeout).isFalse();
        subscriber.assertNoValues();

        scheduler.advanceTimeBy(3, timeUnit);
        subscriber.assertCompleted();
        assertThat(reportAction.hadTimeout).isFalse();
    }

    private static class SubbedReportAction implements Action1<Long> {
        boolean hadTimeout = false;

        @Override
        public void call(Long time) {
            hadTimeout = true;
        }
    }
}
