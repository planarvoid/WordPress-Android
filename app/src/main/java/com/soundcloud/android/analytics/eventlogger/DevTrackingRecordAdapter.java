package com.soundcloud.android.analytics.eventlogger;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.TrackingRecord;

import android.support.v4.util.CircularArray;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class DevTrackingRecordAdapter extends RecyclerView.Adapter<DevTrackingRecordAdapter.TrackingRecordViewHolder> {

    private static final String DATE_FORMAT = "HH:mm:ss a";
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.US);

    private CircularArray<TrackingRecord> trackingRecords = new CircularArray<>();

    @Inject
    DevTrackingRecordAdapter() {}

    @Override
    public TrackingRecordViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.dev_tracking_record, viewGroup, false);
        return new TrackingRecordViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(TrackingRecordViewHolder viewHolder, int position) {
        final TrackingRecord trackingRecord = trackingRecords.get(position);
        final Date date = new Date(trackingRecord.getTimeStamp());
        viewHolder.setTimestamp(dateFormat.format(date));
        viewHolder.setData(trackingRecord.getData());
    }

    @Override
    public int getItemCount() {
        return trackingRecords.size();
    }

    void replaceItems(CircularArray<TrackingRecord> trackingRecords) {
        this.trackingRecords = trackingRecords;
        notifyDataSetChanged();
    }

    static class TrackingRecordViewHolder extends RecyclerView.ViewHolder {
        private final TextView timestamp;
        private final TextView data;

        TrackingRecordViewHolder(View itemView) {
            super(itemView);
            this.timestamp = (TextView) itemView.findViewById(R.id.timestamp);
            this.data = (TextView) itemView.findViewById(R.id.data);
        }

        void setTimestamp(String timestampString) {
            timestamp.setText(timestampString);
        }

        void setData(String dataString) {
            data.setText(dataString);
        }
    }
}
