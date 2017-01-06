package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PerformanceEventTest {

    private static final boolean IS_USER_LOGGED_IN = true;
    private static final long TIME_MILLIS = 10L;
    private static final String APP_VERSION = "debug";
    private static final String DEVICE_NAME = "Xiaomi";
    private static final String ANDROID_VERSION = "7.0.0";

    private PerformanceEvent performanceEvent;

    @Before
    public void setUp() {
        performanceEvent = PerformanceEvent.forApplicationStartupTime(IS_USER_LOGGED_IN, TIME_MILLIS, APP_VERSION,
                DEVICE_NAME, ANDROID_VERSION);
    }

    @Test
    public void createPerformanceEventForApplicationStartupTime() {
        assertThat(performanceEvent.name()).isEqualTo(PerformanceEvent.METRIC_APP_STARTUP_TIME);
        assertThat(performanceEvent.userLoggedIn()).isEqualTo(IS_USER_LOGGED_IN);
        assertThat(performanceEvent.timeMillis()).isEqualTo(TIME_MILLIS);
        assertThat(performanceEvent.appVersionName()).isEqualTo(APP_VERSION);
        assertThat(performanceEvent.deviceName()).isEqualTo(DEVICE_NAME);
        assertThat(performanceEvent.androidVersion()).isEqualTo(ANDROID_VERSION);
    }
}
