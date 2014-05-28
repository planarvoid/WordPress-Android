package com.soundcloud.android.view.adapters;

import com.soundcloud.android.R;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PropertySet;
import com.soundcloud.android.model.TrackProperty;
import com.soundcloud.android.utils.ScTextUtils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

public class TrackItemPresenter implements CellPresenter<PropertySet> {

    private final LayoutInflater layoutInflater;

    @Inject
    public TrackItemPresenter(LayoutInflater layoutInflater) {
        this.layoutInflater = layoutInflater;
    }

    @Override
    public View createItemView(int position, ViewGroup parent) {
        return layoutInflater.inflate(R.layout.track_list_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<PropertySet> trackItems) {
        final PropertySet propertySet = trackItems.get(position);
        getTextView(itemView, R.id.title).setText(propertySet.get(PlayableProperty.TITLE));
        getTextView(itemView, R.id.username).setText(propertySet.get(PlayableProperty.CREATOR));
        final String formattedDuration = ScTextUtils.formatTimestamp(propertySet.get(PlayableProperty.DURATION));
        getTextView(itemView, R.id.duration).setText(formattedDuration);
        getTextView(itemView, R.id.play_count).setText(Long.toString(propertySet.get(TrackProperty.PLAY_COUNT)));

        final TextView reposterView = getTextView(itemView, R.id.reposter);
        if (propertySet.contains(PlayableProperty.REPOSTER)) {
            reposterView.setVisibility(View.VISIBLE);
            reposterView.setText(propertySet.get(PlayableProperty.REPOSTER));
        } else {
            reposterView.setVisibility(View.GONE);
        }
    }

    private TextView getTextView(final View convertView, final int id) {
        return (TextView) convertView.findViewById(id);
    }
}
