package com.soundcloud.android.stream;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.PlayingTrackRenderer;
import com.soundcloud.android.tracks.TrackItem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class StreamTrackItemRenderer implements PlayingTrackRenderer {

    @Inject
    public StreamTrackItemRenderer() {}

    @Override
    public void setPlayingTrack(Urn currentTrackUrn) {
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.stream_track_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<TrackItem> items) {
    }
}
