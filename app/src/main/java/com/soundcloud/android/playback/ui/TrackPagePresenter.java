package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.events.PlaybackProgress;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.playback.ui.progress.ProgressAware;
import com.soundcloud.android.playback.ui.view.WaveformView;
import com.soundcloud.android.view.JaggedTextView;
import com.soundcloud.android.waveform.WaveformOperations;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

import javax.inject.Inject;

class TrackPagePresenter implements View.OnClickListener {

    private final Resources resources;
    private final ImageOperations imageOperations;
    private final WaveformOperations waveformOperations;
    private final Listener listener;

    private boolean isExpanded;

    interface Listener {
        void onTogglePlay();
        void onNext();
        void onPrevious();
        void onFooterTap();
        void onPlayerClose();
    }

    @Inject
    public TrackPagePresenter(Resources resources, ImageOperations imageOperations,
                              WaveformOperations waveformOperations, TrackPageListener listener) {
        this.resources = resources;
        this.imageOperations = imageOperations;
        this.waveformOperations = waveformOperations;
        this.listener = listener;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.footer_toggle:
            case R.id.player_play:
            case R.id.track_page_artwork:
                listener.onTogglePlay();
                break;
            case R.id.track_page_next:
                listener.onNext();
                break;
            case R.id.track_page_previous:
                listener.onPrevious();
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

    public View createTrackPage(ViewGroup container) {
        final View trackView = LayoutInflater.from(container.getContext()).inflate(R.layout.player_track_page, container, false);
        setupHolder(trackView);
        if (isExpanded) {
            setExpandedState(trackView, false);
        } else {
            setCollapsed(trackView);
        }
        return trackView;
    }

    public void populateTrackPage(View trackView, Track track) {
        final TrackPageHolder holder = getViewHolder(trackView);
        holder.user.setText(track.getUserName());
        holder.title.setText(track.getTitle());
        imageOperations.displayInVisualPlayer(track.getUrn(), ApiImageSize.getFullImageSize(resources),
                holder.artwork.getImageView(), holder.artwork.getImageListener());
        holder.waveform.displayWaveform(waveformOperations.waveformDataFor(track));

        holder.footerPlayToggle.setChecked(false); // Reset to paused state
        holder.footerUser.setText(track.getUserName());
        holder.footerTitle.setText(track.getTitle());

        setClickListener(holder.getOnClickViews(), this);
    }

    void populateTrackPage(View trackView, Track track, PlaybackProgress playbackProgress) {
        populateTrackPage(trackView, track);
        for (ProgressAware view : getViewHolder(trackView).getProgressAwareViews()){
            view.setProgress(playbackProgress);
        }
    }

    public void setPlayState(View trackView, Playa.StateTransition state, boolean isCurrentTrack) {
        final TrackPageHolder holder = getViewHolder(trackView);
        final boolean playSessionIsActive = state.playSessionIsActive();

        setVisibility(holder.getPlayControls(), !playSessionIsActive);
        holder.footerPlayToggle.setChecked(playSessionIsActive && isCurrentTrack);
        setArtworkPlayState(holder, state, isCurrentTrack);
        setWaveformPlayState(holder, state, isCurrentTrack);
    }

    private void setArtworkPlayState(TrackPageHolder holder, Playa.StateTransition state, boolean isCurrentTrack) {
        if (state.playSessionIsActive()) {
            final PlaybackProgress progressOnThisTrack = isCurrentTrack && state.isPlayerPlaying() ?
                    state.getProgress() : null;
            holder.artwork.showPlayingState(progressOnThisTrack);
        } else {
            holder.artwork.showIdleState();
        }
        holder.title.showBackground(state.playSessionIsActive());
        holder.user.showBackground(state.playSessionIsActive());
    }

    private void setWaveformPlayState(TrackPageHolder holder, Playa.StateTransition state, boolean isCurrentTrack) {
        if (isCurrentTrack && state.isPlayerPlaying()){
            holder.waveform.showPlayingState(state.getProgress());
        } else {
            holder.waveform.showIdleState();
        }
    }

    public void setProgress(View trackView, PlaybackProgress progress) {
        for (ProgressAware view : getViewHolder(trackView).getProgressAwareViews()){
            view.setProgress(progress);
        }
    }

    public void resetProgress(View trackView) {
        setProgress(trackView, PlaybackProgress.empty());
    }

    public void setExpandedMode(boolean isExpanded) {
        this.isExpanded = isExpanded;
    }

    public void setExpandedState(View trackView, boolean isPlaying) {
        final TrackPageHolder holder = getViewHolder(trackView);
        holder.footer.setVisibility(View.GONE);
        holder.waveform.setVisibility(isPlaying ? View.VISIBLE : View.GONE);
        holder.artwork.lighten();
        setVisibility(holder.getFullScreenViews(), true);
    }

    public void setCollapsed(View trackView) {
        final TrackPageHolder holder = getViewHolder(trackView);
        holder.footer.setVisibility(View.VISIBLE);
        holder.artwork.darken();
        setVisibility(holder.getFullScreenViews(), false);
    }

    private void setVisibility(View[] views, boolean visible) {
        for (View v : views) {
            v.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void setClickListener(View[] views, View.OnClickListener listener) {
        for (View v : views) {
            v.setOnClickListener(listener);
        }
    }

    private TrackPageHolder getViewHolder(View trackView) {
        return (TrackPageHolder) trackView.getTag();
    }

    private void setupHolder(View trackView) {
        TrackPageHolder holder = new TrackPageHolder();
        holder.title = (JaggedTextView) trackView.findViewById(R.id.track_page_title);
        holder.user = (JaggedTextView) trackView.findViewById(R.id.track_page_user);
        holder.artwork = (PlayerArtworkView) trackView.findViewById(R.id.track_page_artwork);
        holder.waveform = (WaveformView) trackView.findViewById(R.id.track_page_waveform);
        holder.likeToggle = (ToggleButton) trackView.findViewById(R.id.track_page_like);
        holder.more = trackView.findViewById(R.id.track_page_more);
        holder.close = trackView.findViewById(R.id.player_close);
        holder.bottomClose = trackView.findViewById(R.id.player_bottom_close);
        holder.nextTouch = trackView.findViewById(R.id.track_page_next);
        holder.nextButton = trackView.findViewById(R.id.player_next);
        holder.previousTouch = trackView.findViewById(R.id.track_page_previous);
        holder.previousButton = trackView.findViewById(R.id.player_previous);
        holder.playButton = trackView.findViewById(R.id.player_play);

        holder.footer = trackView.findViewById(R.id.footer_controls);
        holder.footerPlayToggle = (ToggleButton) trackView.findViewById(R.id.footer_toggle);
        holder.footerTitle = (TextView) trackView.findViewById(R.id.footer_title);
        holder.footerUser = (TextView) trackView.findViewById(R.id.footer_user);
        trackView.setTag(holder);
    }

    static class TrackPageHolder {
        // Expanded player
        JaggedTextView title;
        JaggedTextView user;
        PlayerArtworkView artwork;
        WaveformView waveform;
        ToggleButton likeToggle;
        View more;
        View close;
        View bottomClose;
        View nextTouch;
        View nextButton;
        View previousTouch;
        View previousButton;
        View playButton;
        // Footer player
        View footer;
        ToggleButton footerPlayToggle;
        TextView footerTitle;
        TextView footerUser;

        public View[] getOnClickViews() {
            return new View[] { artwork, close, bottomClose, nextTouch, previousTouch, playButton, footer, footerPlayToggle };
        }

        public View[] getFullScreenViews() {
            return new View[] { title, user, close, waveform };
        }

        public View[] getPlayControls() {
            return new View[] { nextButton, previousButton, playButton };
        }

        public ProgressAware[] getProgressAwareViews() {
            return new ProgressAware[] { waveform, artwork };
        }
    }

}
