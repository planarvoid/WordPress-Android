package com.soundcloud.android.analytics.eventlogger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.TrackingRecord;
import com.soundcloud.android.analytics.eventlogger.DevTrackingRecordAdapter.TrackingRecordViewHolder;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;

import android.support.v4.util.CircularArray;
import android.view.LayoutInflater;
import android.view.View;

public class DevTrackingRecordAdapterTest extends AndroidUnitTest {

    private static final long TIMESTAMP = 123L;
    private static final String BACKEND = "backend";
    private static final String DATA = "data";

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
        final View itemView = LayoutInflater.from(context()).inflate(R.layout.dev_tracking_record, null, false);
        final TrackingRecordViewHolder viewHolder = new TrackingRecordViewHolder(itemView);
        setTrackingRecords();

        adapter.onBindViewHolder(viewHolder, 0);

        assertThat(viewHolder.timestamp.getText()).isNotEmpty();
        assertThat(viewHolder.data.getText()).isEqualTo(DATA);
    }

    @Test
    public void shouldNotHandleOnItemClicked() {
        final View itemView = LayoutInflater.from(context()).inflate(R.layout.dev_tracking_record, null, false);
        final TrackingRecordViewHolder viewHolder = new TrackingRecordViewHolder(itemView);
        final DevTrackingRecordAdapter.Listener listener = mock(DevTrackingRecordAdapter.Listener.class);
        setTrackingRecords();

        adapter.onBindViewHolder(viewHolder, 0);
        viewHolder.itemView.performClick();

        verify(listener, never()).onItemClicked(trackingRecords.getLast());
    }

    @Test
    public void shouldHandleOnItemClicked() {
        final View itemView = LayoutInflater.from(context()).inflate(R.layout.dev_tracking_record, null, false);
        final TrackingRecordViewHolder viewHolder = new TrackingRecordViewHolder(itemView);
        final DevTrackingRecordAdapter.Listener listener = mock(DevTrackingRecordAdapter.Listener.class);
        setTrackingRecords();

        adapter.setListener(listener);
        adapter.onBindViewHolder(viewHolder, 0);
        viewHolder.itemView.performClick();

        verify(listener).onItemClicked(trackingRecords.getLast());
    }

    private void setTrackingRecords() {
        final TrackingRecord trackingRecord = new TrackingRecord(TIMESTAMP, BACKEND, DATA);
        trackingRecords.addLast(trackingRecord);
        adapter.replaceItems(trackingRecords);
    }
}
