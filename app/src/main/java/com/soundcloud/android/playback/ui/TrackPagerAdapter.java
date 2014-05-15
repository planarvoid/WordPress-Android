package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.view.RecyclingPager.RecyclingPagerAdapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;

public class TrackPagerAdapter extends RecyclingPagerAdapter {

    private final PlayQueueManager playQueueManager;

    @Inject
    TrackPagerAdapter(PlayQueueManager playQueueManager) {
        this.playQueueManager = playQueueManager;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup container) {
        if (convertView == null) {
            convertView = LayoutInflater.from(container.getContext()).inflate(R.layout.player_track_page, null);
        }

        ((TextView) convertView.findViewById(R.id.track_page_title)).setText(Urn.forTrack(playQueueManager.getIdAtPosition(position)).toString());

        return convertView;
    }

    @Override
    public int getItemViewTypeFromObject(Object object) {
        return 0;
    }

    @Override
    public int getCount() {
        return playQueueManager.getCurrentPlayQueueSize();
    }
}
