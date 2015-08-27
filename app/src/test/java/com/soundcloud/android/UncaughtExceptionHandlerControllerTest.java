package com.soundcloud.android;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.MemoryReporter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;

import java.util.concurrent.TimeUnit;

public class UncaughtExceptionHandlerControllerTest extends AndroidUnitTest {

    @Mock private Thread.UncaughtExceptionHandler anOtherHandler;
    @Mock private MemoryReporter memoryReporter;
    private UncaughtExceptionHandlerController controller;
    private TestScheduler scheduler;

    @Before
    public void setUp() throws Exception {
        scheduler = Schedulers.test();
        controller = new UncaughtExceptionHandlerController(scheduler, memoryReporter);
    }

    @Test
    public void resetHandlerAndReportErrorWhenHandlerUnset() {
        controller.setupUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(anOtherHandler);
        scheduler.advanceTimeBy(2, TimeUnit.MINUTES);

        Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), new OutOfMemoryError());
        verify(memoryReporter).reportOomStats();
    }
}