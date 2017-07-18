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

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@RunWith(MockitoJUnitRunner.class)
public class SlowQueryReporterTest {

    @Mock private DebugStorage debugStorage;

    private SlowQueryReporter slowQueryReporter;
    private TestScheduler scheduler = new TestScheduler();
    private TestObserver<SlowQueryReporter.SlowQueryOutput> reporter = new TestObserver<>();
    private DebugDatabaseStat op1 = DebugDatabaseStat.create("op1", SlowQueryReporter.LENGTH_TOLERANCE_MS + 1);

    @Before
    public void setUp() throws Exception {
        when(debugStorage.tableSizes()).thenReturn(Observable.just(Pair.of("table1", 1), Pair.of("table2", 2)));

        this.slowQueryReporter = new SlowQueryReporter(debugStorage,
                                                       scheduler,
                                                       reporter);

    }

    @Test
    public void doesNotReportIfNotSlow() throws Exception {
        slowQueryReporter.reportIfSlow(DebugDatabaseStat.create("op1", 1));

        reporter.assertNoValues();
    }

    @Test
    public void reportsIfSlow() throws Exception {
        slowQueryReporter.reportIfSlow(op1);

        scheduler.triggerActions();

        reporter.assertValue(SlowQueryReporter.SlowQueryOutput.create("1 [table1]\n2 [table2]\n", Collections.singletonList(op1)));
    }

    @Test
    public void reportsOnceThrottled() throws Exception {
        slowQueryReporter.reportIfSlow(op1);
        scheduler.triggerActions();
        slowQueryReporter.reportIfSlow(DebugDatabaseStat.create("op2", SlowQueryReporter.LENGTH_TOLERANCE_MS + 1));
        scheduler.advanceTimeBy(SlowQueryReporter.THROTTLE_TIME_MS, TimeUnit.MILLISECONDS);

        reporter.assertValue(SlowQueryReporter.SlowQueryOutput.create("1 [table1]\n2 [table2]\n", Collections.singletonList(op1)));
    }

}
