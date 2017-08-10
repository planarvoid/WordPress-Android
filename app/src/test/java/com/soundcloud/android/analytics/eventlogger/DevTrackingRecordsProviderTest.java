package com.soundcloud.android.analytics.eventlogger;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.analytics.TrackingRecord;
import io.reactivex.observers.TestObserver;
import org.junit.Before;
import org.junit.Test;

public class DevTrackingRecordsProviderTest {

    private DevTrackingRecordsProvider devTrackingRecordsProvider;

    @Before
    public void setUp() {
        devTrackingRecordsProvider = new DevTrackingRecordsProvider();
    }

    @Test
    public void shouldAdd() {
        final TrackingRecord trackingRecord = new TrackingRecord(123L, "backend", "data");
        final TestObserver<DevTrackingRecordsProvider.Action> subscriber = devTrackingRecordsProvider.action().test();

        devTrackingRecordsProvider.add(trackingRecord);

        subscriber.assertValues(DevTrackingRecordsProvider.Action.DEFAULT, DevTrackingRecordsProvider.Action.ADD);
        assertThat(devTrackingRecordsProvider.latest().get(0)).isEqualTo(trackingRecord);
    }

    @Test
    public void shouldDeleteAll() {
        final TestObserver<DevTrackingRecordsProvider.Action> subscriber = devTrackingRecordsProvider.action().test();

        devTrackingRecordsProvider.deleteAll();

        subscriber.assertValues(DevTrackingRecordsProvider.Action.DEFAULT, DevTrackingRecordsProvider.Action.DELETE_ALL);
        assertThat(devTrackingRecordsProvider.latest().size()).isEqualTo(0);
    }
}
