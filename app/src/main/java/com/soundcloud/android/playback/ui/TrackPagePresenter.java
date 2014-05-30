package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.view.JaggedTextView;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        void onPlayerClose();
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
            case R.id.track_page_artwork:
                listener.onTogglePlay();
                break;
            case R.id.footer_controls:
                listener.onFooterTap();
                break;
            case R.id.player_close:
            case R.id.player_bottom_close:
                listener.onPlayerClose();
                break;
            default:
                throw new IllegalArgumentException("Unexpected view ID");
        }
    }

    public View createTrackPage(ViewGroup container, boolean fullScreen) {
        View trackView = LayoutInflater.from(container.getContext()).inflate(R.layout.player_track_page, container, false);
        setupHolder(trackView);
        setFullScreen(trackView, fullScreen);
        return trackView;
    }

    public void setProgress(View trackView, PlaybackProgressEvent progress) {
        getViewHolder(trackView).artwork.setProgressProportion(progress.getProgressProportion());
    }

    public void setPlayState(View trackView, boolean isPlaying) {
        getViewHolder(trackView).footerPlayToggle.setChecked(isPlaying);
    }

    public void setFullScreen(View trackView, boolean expanded) {
        TrackPageHolder holder = getViewHolder(trackView);
        holder.footer.setVisibility(expanded ? View.GONE : View.VISIBLE);
        holder.user.setVisibility(expanded ? View.VISIBLE : View.GONE);
        holder.title.setVisibility(expanded ? View.VISIBLE : View.GONE);
        holder.close.setVisibility(expanded ? View.VISIBLE : View.GONE);
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
        imageOperations.displayInVisualPlayer(track.getUrn(), ApiImageSize.getFullImageSize(resources), holder.artwork);
        holder.artwork.setProgressProportion(currentProgressProportion);
        holder.artwork.setOnClickListener(this);
        holder.close.setOnClickListener(this);
        holder.bottomClose.setOnClickListener(this);

        holder.footer.setOnClickListener(this);
        holder.footerPlayToggle.setOnClickListener(this);
        holder.footerPlayToggle.setChecked(false); // Reset to paused state
        holder.footerUser.setText(track.getUserName());
        holder.footerTitle.setText(track.getTitle());
    }

    private TrackPageHolder getViewHolder(View trackView) {
        return (TrackPageHolder) trackView.getTag();
    }

    private void setupHolder(View trackView) {
        TrackPageHolder holder = new TrackPageHolder();
        holder.title = (JaggedTextView) trackView.findViewById(R.id.track_page_title);
        holder.user = (JaggedTextView) trackView.findViewById(R.id.track_page_user);
        holder.artwork = (PlayerArtworkImageView) trackView.findViewById(R.id.track_page_artwork);
        holder.likeToggle = (ToggleButton) trackView.findViewById(R.id.track_page_like);
        holder.more = trackView.findViewById(R.id.track_page_more);
        holder.close = trackView.findViewById(R.id.player_close);
        holder.bottomClose = trackView.findViewById(R.id.player_bottom_close);

        holder.footer = trackView.findViewById(R.id.footer_controls);
        holder.footerPlayToggle = (ToggleButton) trackView.findViewById(R.id.footer_toggle);
        holder.footerTitle = (TextView) trackView.findViewById(R.id.footer_title);
        holder.footerUser = (TextView) trackView.findViewById(R.id.footer_user);
        trackView.setTag(holder);
    }

    static class TrackPageHolder {
        JaggedTextView title;
        JaggedTextView user;
        PlayerArtworkImageView artwork;
        ToggleButton likeToggle;
        View more;
        View close;
        View bottomClose;

        View footer;
        ToggleButton footerPlayToggle;
        TextView footerTitle;
        TextView footerUser;
    }

}
