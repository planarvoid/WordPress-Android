package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageSize;
import com.soundcloud.android.model.Track;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;

public class TrackPagePresenter {

    private final ImageOperations imageOperations;
    private final Resources resources;

    @Inject
    public TrackPagePresenter(ImageOperations imageOperations, Resources resources) {
        this.imageOperations = imageOperations;
        this.resources = resources;
    }

    public View createTrackPage(ViewGroup container){
        return LayoutInflater.from(container.getContext()).inflate(R.layout.player_track_page, container, false);
    }

    public void setProgressOnTrackView(View trackView, PlaybackProgressEvent progress) {
        ((PlayerArtworkImageView) trackView.findViewById(R.id.artwork)).setProgressProportion(progress.getProgressProportion());
    }

    public void populateTrackPage(View trackView, Track track) {
        populateTrackPage(trackView, track, 0);
    }

    public void populateTrackPage(View trackView, Track track, PlaybackProgressEvent currentProgress) {
        populateTrackPage(trackView, track, currentProgress == null ? 0 : currentProgress.getProgressProportion());
    }

    private void populateTrackPage(View trackView, Track track, float currentProgressProportion) {
        ((TextView) trackView.findViewById(R.id.track_page_user)).setText(track.getUserName());
        ((TextView) trackView.findViewById(R.id.track_page_title)).setText(track.getTitle());

        final PlayerArtworkImageView artworkView = (PlayerArtworkImageView) trackView.findViewById(R.id.artwork);
        imageOperations.displayInVisualPlayer(track.getUrn(), ImageSize.getFullImageSize(resources), artworkView);
        artworkView.setProgressProportion(currentProgressProportion);
    }
}
