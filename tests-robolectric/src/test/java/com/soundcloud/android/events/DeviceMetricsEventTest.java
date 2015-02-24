package com.soundcloud.android.events;

import static com.pivotallabs.greatexpectations.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class DeviceMetricsEventTest {
    @Test
    public void shouldTrackDeviceMetricsEventForDatabaseSizeLessThanOneMb() {
        verifyDatabaseSizeEvent(1, "<1mb");
    }

    @Test
    public void shouldTrackDeviceMetricsEventForDatabaseSizeBetweenOneAndFiveMb() {
        verifyDatabaseSizeEvent(1024 * 1024 + 1, "1mb to 5mb");
    }

    @Test
    public void shouldTrackDeviceMetricsEventForDatabaseSizeBetweenFiveAndTenMb() {
        verifyDatabaseSizeEvent(1024 * 1024 * 5 + 1,"5mb to 10mb");
    }

    @Test
    public void shouldTrackDeviceMetricsEventForDatabaseSizeBetweenTenAndTwentyMb() {
        verifyDatabaseSizeEvent(1024 * 1024 * 10 + 1,"10mb to 20mb");
    }

    @Test
    public void shouldTrackDeviceMetricsEventForDatabaseSizeBetweenTwentyAndFiftyMb() {
        verifyDatabaseSizeEvent(1024 * 1024 * 20 + 1,"20mb to 50mb");
    }

    @Test
    public void shouldTrackDeviceMetricsEventForDatabaseSizeBetweenFiftyAndOneHundredMb() {
        verifyDatabaseSizeEvent(1024 * 1024 * 50 + 1,"50mb to 100mb");
    }

    @Test
    public void shouldTrackDeviceMetricsEventForDatabaseSizeBetweenOneHundredAndTwoHundredMb() {
        verifyDatabaseSizeEvent(1024 * 1024 * 100 + 1,"100mb to 200mb");
    }

    @Test
    public void shouldTrackDeviceMetricsEventForDatabaseSizeBetweenTwoHundredAndFiveHundredMb() {
        verifyDatabaseSizeEvent(1024 * 1024 * 200 + 1,"200mb to 500mb");
    }

    @Test
    public void shouldTrackDeviceMetricsEventForDatabaseSizeGreaterThanOneHundredMb() {
        verifyDatabaseSizeEvent(1024 * 1024 * 500 + 1,">500mb");
    }

    @SuppressWarnings("unchecked")
    private void verifyDatabaseSizeEvent(long dbSize, String eventSizeReport){
        final TrackingEvent event = DeviceMetricsEvent.forDatabaseSize(dbSize);
        expect(event.get(DeviceMetricsEvent.KEY_DATABASE)).toEqual(eventSizeReport);
    }
}