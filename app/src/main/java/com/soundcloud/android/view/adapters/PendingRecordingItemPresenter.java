package com.soundcloud.android.view.adapters;

import com.soundcloud.android.R;
import com.soundcloud.android.api.legacy.model.Recording;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

public class PendingRecordingItemPresenter implements CellPresenter<Recording> {

    private final Resources resources;
    private final LayoutInflater layoutInflater;

    @Inject
    public PendingRecordingItemPresenter(Resources resources, LayoutInflater layoutInflater) {
        this.resources = resources;
        this.layoutInflater = layoutInflater;
    }

    @Override
    public View createItemView(int position, ViewGroup parent) {
        return layoutInflater.inflate(R.layout.recording_list_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<Recording> trackItems) {
        final Recording recording = trackItems.get(position);
        getTextView(itemView, R.id.list_item_header).setText(recording.getStatusMessage(resources));
        getTextView(itemView, R.id.list_item_subheader).setText(recording.getTitle(resources));
        getTextView(itemView, R.id.list_item_right_info).setText(recording.formattedDuration());
        getTextView(itemView, R.id.time_since_recorded).setText(ScTextUtils.formatTimeElapsedSince(resources, recording.lastModified(), true));
        ((ImageView) itemView.findViewById(R.id.image)).setImageResource(R.drawable.placeholder_local_recordings);

        showRelevantAdditionalInformation(itemView, recording);

        setTextColor(itemView);
    }

    private void setTextColor(View itemView) {
        int textColor = resources.getColor(R.color.record_list_item_text);
        getTextView(itemView, R.id.list_item_right_info).setTextColor(textColor);
        getTextView(itemView, R.id.list_item_header).setTextColor(textColor);
    }

    private void showRelevantAdditionalInformation(View itemView, Recording recording) {
        getTextView(itemView, R.id.private_indicator).setVisibility(View.GONE);
        if (recording.is_private) {
            showPrivateIndicator(itemView);
        }
    }

    private void showPrivateIndicator(View itemView) {
        getTextView(itemView, R.id.private_indicator).setVisibility(View.VISIBLE);
    }

    private TextView getTextView(final View convertView, final int id) {
        return (TextView) convertView.findViewById(id);
    }
}
