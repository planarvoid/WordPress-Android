package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Track;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;

public class TrackPagePresenter {

    @Inject
    public TrackPagePresenter() {
    }


    public void populateTrackPage(View trackView, Track track) {
        ((TextView) trackView.findViewById(R.id.track_page_title)).setText(track.getTitle());
    }

    public View createTrackPage(ViewGroup container){
        return LayoutInflater.from(container.getContext()).inflate(R.layout.player_track_page, container, false);
    }


}
