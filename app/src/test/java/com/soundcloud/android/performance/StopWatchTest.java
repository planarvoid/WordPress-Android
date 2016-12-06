package com.soundcloud.android.performance;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StopWatchTest {

    private StopWatch stopWatch;

    @Before
    public void setUp() {
        stopWatch = new StopWatch();
    }

    @Test
    public void mustResetStopWatch() {
        stopWatch.reset();

        assertThat(stopWatch.getTotalTimeMillis()).isZero();
    }

    @Test
    public void mustStartStopWatch() throws InterruptedException {
        stopWatch.start();
        Thread.sleep(5);
        stopWatch.stop();

        assertThat(stopWatch.getTotalTimeMillis()).isGreaterThan(0L);
    }
}
