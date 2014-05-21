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
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import javax.inject.Inject;

class TrackPagePresenter implements View.OnClickListener {

    private final ImageOperations imageOperations;
    private final Resources resources;

    private Listener listener;

    interface Listener {
        void onTogglePlay();
        void onFooterTap();
    }

    @Inject
    public TrackPagePresenter(ImageOperations imageOperations, Resources resources) {
        this.imageOperations = imageOperations;
        this.resources = resources;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.footer_toggle:
                listener.onTogglePlay();
                break;
            case R.id.footer_controls:
                listener.onFooterTap();
                break;
            default:
                throw new IllegalArgumentException("Unexpected view ID");
        }
    }

    public View createTrackPage(ViewGroup container) {
        return LayoutInflater.from(container.getContext()).inflate(R.layout.player_track_page, container, false);
    }

    public void setProgress(View trackView, PlaybackProgressEvent progress) {
        getViewHolder(trackView).artwork.setProgressProportion(progress.getProgressProportion());
    }

    public void setPlayState(View trackView, boolean isPlaying) {
        getViewHolder(trackView).footerPlayToggle.setChecked(isPlaying);
    }

    public void populateTrackPage(View trackView, Track track) {
        populateTrackPage(trackView, track, 0);
    }

    public void populateTrackPage(View trackView, Track track, PlaybackProgressEvent currentProgress) {
        populateTrackPage(trackView, track, currentProgress == null ? 0 : currentProgress.getProgressProportion());
    }

    private void populateTrackPage(View trackView, Track track, float currentProgressProportion) {
        TrackPageHolder holder = getViewHolder(trackView);
        holder.user.setText(track.getUserName());
        holder.title.setText(track.getTitle());
        imageOperations.displayInVisualPlayer(track.getUrn(), ImageSize.getFullImageSize(resources), holder.artwork);
        holder.artwork.setProgressProportion(currentProgressProportion);

        holder.footer.setOnClickListener(this);
        holder.footerPlayToggle.setOnClickListener(this);
        holder.footerPlayToggle.setChecked(false); // Reset to paused state
        holder.footerUser.setText(track.getUserName());
        holder.footerTitle.setText(track.getTitle());
    }

    private TrackPageHolder getViewHolder(View trackView) {
        if (trackView.getTag() == null) {
            setupHolder(trackView);
        }
        return (TrackPageHolder) trackView.getTag();
    }

    private void setupHolder(View trackView) {
        TrackPageHolder holder = new TrackPageHolder();
        holder.title = (TextView) trackView.findViewById(R.id.track_page_title);
        holder.user = (TextView) trackView.findViewById(R.id.track_page_user);
        holder.likeToggle = (ToggleButton) trackView.findViewById(R.id.track_page_like);
        holder.artwork = (PlayerArtworkImageView) trackView.findViewById(R.id.track_page_artwork);
        holder.close = (Button) trackView.findViewById(R.id.player_close);

        holder.footer = trackView.findViewById(R.id.footer_controls);
        holder.footerPlayToggle = (ToggleButton) trackView.findViewById(R.id.footer_toggle);
        holder.footerTitle = (TextView) trackView.findViewById(R.id.footer_title);
        holder.footerUser = (TextView) trackView.findViewById(R.id.footer_user);
        trackView.setTag(holder);
    }

    static class TrackPageHolder {
        TextView title;
        TextView user;
        ToggleButton likeToggle;
        PlayerArtworkImageView artwork;
        Button close;

        View footer;
        ToggleButton footerPlayToggle;
        TextView footerTitle;
        TextView footerUser;
    }

}
