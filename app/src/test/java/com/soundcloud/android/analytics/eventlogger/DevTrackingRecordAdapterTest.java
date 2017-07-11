package com.soundcloud.android.analytics.eventlogger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.analytics.TrackingRecord;
import com.soundcloud.android.analytics.eventlogger.DevTrackingRecordAdapter.TrackingRecordViewHolder;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;

import android.support.v4.util.CircularArray;

public class DevTrackingRecordAdapterTest extends AndroidUnitTest {

    private DevTrackingRecordAdapter adapter;
    private CircularArray<TrackingRecord> trackingRecords = new CircularArray<>();

    @Before
    public void setUp() {
        adapter = new DevTrackingRecordAdapter();
    }

    @Test
    public void shouldGetItemCount() {
        setTrackingRecords();
        assertThat(adapter.getItemCount()).isEqualTo(1);
    }

    @Test
    public void shouldBindViewHolder() {
        final TrackingRecordViewHolder viewHolder = mock(TrackingRecordViewHolder.class);
        setTrackingRecords();

        adapter.onBindViewHolder(viewHolder, 0);

        verify(viewHolder).setTimestamp(anyString());
        verify(viewHolder).setData(anyString());
    }

    private void setTrackingRecords() {
        final TrackingRecord trackingRecord = new TrackingRecord(123L, "backend", "data");
        trackingRecords.addLast(trackingRecord);
        adapter.replaceItems(trackingRecords);
    }
}
