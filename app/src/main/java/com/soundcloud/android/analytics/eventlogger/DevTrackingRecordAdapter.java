package com.soundcloud.android.analytics.eventlogger;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.TrackingRecord;
import com.soundcloud.java.optional.Optional;

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
    private Optional<Listener> listenerOptional = Optional.absent();

    interface Listener {
        void onItemClicked(TrackingRecord trackingRecord);
    }

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
        viewHolder.timestamp.setText(dateFormat.format(date));
        viewHolder.data.setText(trackingRecord.getData());
        listenerOptional.ifPresent(listener -> viewHolder.itemView.setOnClickListener(v -> listener.onItemClicked(trackingRecord)));
    }

    @Override
    public int getItemCount() {
        return trackingRecords.size();
    }

    public void setListener(Listener listener) {
        this.listenerOptional = Optional.of(listener);
    }

    void replaceItems(CircularArray<TrackingRecord> trackingRecords) {
        this.trackingRecords = trackingRecords;
        notifyDataSetChanged();
    }

    static class TrackingRecordViewHolder extends RecyclerView.ViewHolder {
        final TextView timestamp;
        final TextView data;

        TrackingRecordViewHolder(View itemView) {
            super(itemView);
            this.timestamp = itemView.findViewById(R.id.timestamp);
            this.data = itemView.findViewById(R.id.data);
        }
    }
}
