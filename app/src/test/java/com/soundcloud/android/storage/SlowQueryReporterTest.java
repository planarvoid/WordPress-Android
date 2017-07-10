package com.soundcloud.android.storage;

import static org.mockito.Mockito.when;

import com.soundcloud.java.collections.Pair;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.TestScheduler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.TimeUnit;

@RunWith(MockitoJUnitRunner.class)
public class SlowQueryReporterTest {

    @Mock private DebugStorage debugStorage;

    private SlowQueryReporter slowQueryReporter;
    private TestScheduler scheduler = new TestScheduler();
    private TestObserver<String> reporter = new TestObserver<>();

    @Before
    public void setUp() throws Exception {
        when(debugStorage.tableSizes()).thenReturn(Observable.just(Pair.of("table1", 1), Pair.of("table2", 2)));

        this.slowQueryReporter = new SlowQueryReporter(debugStorage,
                                                       scheduler,
                                                       reporter);

    }

    @Test
    public void doesNotReportIfNotSlow() throws Exception {
        slowQueryReporter.reportIfSlow(1);

        reporter.assertNoValues();
    }

    @Test
    public void reportsIfSlow() throws Exception {
        slowQueryReporter.reportIfSlow(SlowQueryReporter.LENGTH_TOLERANCE_MS + 1);

        scheduler.triggerActions();

        reporter.assertValue("1 [table1]\n2 [table2]\n");
    }

    @Test
    public void reportsOnceThrottled() throws Exception {
        slowQueryReporter.reportIfSlow(SlowQueryReporter.LENGTH_TOLERANCE_MS + 1);
        slowQueryReporter.reportIfSlow(SlowQueryReporter.LENGTH_TOLERANCE_MS + 1);

        scheduler.advanceTimeBy(SlowQueryReporter.THROTTLE_TIME_MS, TimeUnit.MILLISECONDS);

        reporter.assertValue("1 [table1]\n2 [table2]\n");
    }

    @Test
    public void reportsTwiceAfterThrottled() throws Exception {
        slowQueryReporter.reportIfSlow(SlowQueryReporter.LENGTH_TOLERANCE_MS + 1);
        slowQueryReporter.reportIfSlow(SlowQueryReporter.LENGTH_TOLERANCE_MS + 1);

        scheduler.advanceTimeBy(SlowQueryReporter.THROTTLE_TIME_MS + 1, TimeUnit.MILLISECONDS);

        slowQueryReporter.reportIfSlow(SlowQueryReporter.LENGTH_TOLERANCE_MS + 1);
        scheduler.advanceTimeBy(SlowQueryReporter.THROTTLE_TIME_MS + 1, TimeUnit.MILLISECONDS);
        scheduler.triggerActions();

        reporter.assertValues("1 [table1]\n2 [table2]\n","1 [table1]\n2 [table2]\n");
    }
}
