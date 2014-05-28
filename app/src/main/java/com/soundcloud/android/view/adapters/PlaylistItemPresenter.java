package com.soundcloud.android.view.adapters;

import com.soundcloud.android.R;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PlaylistProperty;
import com.soundcloud.android.model.PropertySet;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

public class PlaylistItemPresenter implements CellPresenter<PropertySet> {

    private final LayoutInflater layoutInflater;
    private final Resources resources;

    @Inject
    public PlaylistItemPresenter(LayoutInflater layoutInflater, Resources resources) {
        this.layoutInflater = layoutInflater;
        this.resources = resources;
    }

    @Override
    public View createItemView(int position, ViewGroup parent) {
        return layoutInflater.inflate(R.layout.playlist_list_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<PropertySet> trackItems) {
        final PropertySet propertySet = trackItems.get(position);
        getTextView(itemView, R.id.title).setText(propertySet.get(PlayableProperty.TITLE));
        getTextView(itemView, R.id.username).setText(propertySet.get(PlayableProperty.CREATOR));
        final int trackCount = propertySet.get(PlaylistProperty.TRACK_COUNT);
        final String numberOfTracks = resources.getQuantityString(R.plurals.number_of_sounds, trackCount, trackCount);
        getTextView(itemView, R.id.track_count).setText(numberOfTracks);

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
