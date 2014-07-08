package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.playback.service.Playa.StateTransition;
import static com.soundcloud.android.playback.ui.PlayerArtworkController.PlayerArtworkControllerFactory;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ui.progress.ProgressAware;
import com.soundcloud.android.playback.ui.progress.ScrubController;
import com.soundcloud.android.playback.ui.view.WaveformView;
import com.soundcloud.android.playback.ui.view.WaveformViewController;
import com.soundcloud.android.playback.ui.view.WaveformViewControllerFactory;
import com.soundcloud.android.view.JaggedTextView;
import com.soundcloud.android.waveform.WaveformOperations;

import android.content.res.Resources;
import android.support.v7.widget.PopupMenu;
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
    private final WaveformViewControllerFactory waveformControllerFactory;
    private final PlayerArtworkControllerFactory artworkControllerFactory;

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
                              WaveformOperations waveformOperations, TrackPageListener listener,
                              WaveformViewControllerFactory waveformControllerFactory,
                              PlayerArtworkControllerFactory artworkControllerFactory) {
        this.resources = resources;
        this.imageOperations = imageOperations;
        this.waveformOperations = waveformOperations;
        this.listener = listener;
        this.waveformControllerFactory = waveformControllerFactory;
        this.artworkControllerFactory = artworkControllerFactory;
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
            setExpanded(trackView, false);
        } else {
            setCollapsed(trackView);
        }
        return trackView;
    }

    void populateTrackPage(View trackView, PlayerTrack track, PlaybackProgress playbackProgress) {
        final TrackPageHolder holder = getViewHolder(trackView);
        holder.user.setText(track.getUserName());
        holder.title.setText(track.getTitle());
        imageOperations.displayInVisualPlayer(track.getUrn(), ApiImageSize.getFullImageSize(resources),
                holder.artworkController.getImageView(), holder.artworkController.getImageListener());
        holder.waveformController.displayWaveform(waveformOperations.waveformDataFor(track.getUrn(), track.getWaveformUrl()));

        holder.footerUser.setText(track.getUserName());
        holder.footerTitle.setText(track.getTitle());

        setClickListener(holder.getOnClickViews(), this);
        setProgress(trackView, playbackProgress);
    }

    public void setPlayState(View trackView, StateTransition stateTransition, boolean isCurrentTrack) {
        final TrackPageHolder holder = getViewHolder(trackView);
        final boolean playSessionIsActive = stateTransition.playSessionIsActive();

        setVisibility(holder.getPlayControls(), !playSessionIsActive);
        holder.footerPlayToggle.setChecked(playSessionIsActive && isCurrentTrack);
        setWaveformPlayState(holder, stateTransition, isCurrentTrack);
        setArtworkPlayState(holder, stateTransition, isCurrentTrack);
    }

    private void setWaveformPlayState(TrackPageHolder holder, StateTransition state, boolean isCurrentTrack) {
        if (isCurrentTrack && state.playSessionIsActive()) {
            if (state.isPlayerPlaying()) {
                holder.waveformController.showPlayingState(state.getProgress());
            } else {
                holder.waveformController.showBufferingState();
            }
        } else {
            holder.waveformController.showIdleState();
        }
    }

    private void setArtworkPlayState(TrackPageHolder holder, StateTransition stateTransition, boolean isCurrentTrack) {
        if (stateTransition.playSessionIsActive()){
            if (isCurrentTrack && stateTransition.isPlayerPlaying()){
                holder.artworkController.showPlayingState(stateTransition.getProgress());
            } else {
                holder.artworkController.showSessionActiveState();
            }

            holder.title.showBackground(true);
            holder.user.showBackground(true);
        } else {
            holder.artworkController.showIdleState();
            holder.title.showBackground(false);
            holder.user.showBackground(false);
        }
    }

    public void reset(View trackView) {
        getViewHolder(trackView).waveformController.scrubStateChanged(ScrubController.SCRUB_STATE_NONE);
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

    public void setExpanded(View trackView, boolean isPlaying) {
        TrackPageHolder holder = getViewHolder(trackView);
        holder.footer.setVisibility(View.GONE);
        holder.waveformController.setWaveformVisibility(isPlaying);
        holder.artworkController.lighten();
        holder.waveformController.setWaveformVisibility(true);
        setVisibility(holder.getFullScreenViews(), true);
    }

    public void setCollapsed(View trackView) {
        TrackPageHolder holder = getViewHolder(trackView);
        holder.footer.setVisibility(View.VISIBLE);
        holder.artworkController.darken();
        holder.waveformController.setWaveformVisibility(false);
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
        holder.artworkView = (PlayerArtworkView) trackView.findViewById(R.id.track_page_artwork);
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

        final WaveformView waveform = (WaveformView) trackView.findViewById(R.id.track_page_waveform);
        holder.waveformController = waveformControllerFactory.create(waveform);
        holder.artworkController = artworkControllerFactory.create(holder.artworkView);
        holder.waveformController.addScrubListener(holder.artworkController);

        final PopupMenu popupMenu = new PopupMenu(trackView.getContext(), holder.more);
        popupMenu.inflate(R.menu.player_page_actions);
        holder.more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupMenu.show();
            }
        });
        trackView.setTag(holder);
    }

    static class TrackPageHolder {
        // Expanded player
        JaggedTextView title;
        JaggedTextView user;
        WaveformViewController waveformController;
        PlayerArtworkView artworkView;
        PlayerArtworkController artworkController;
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
            return new View[] { artworkView, close, bottomClose, nextTouch, previousTouch, playButton, footer, footerPlayToggle };
        }

        public View[] getFullScreenViews() {
            return new View[] { title, user, close };
        }

        public View[] getPlayControls() {
            return new View[] { nextButton, previousButton, playButton };
        }

        public ProgressAware[] getProgressAwareViews() {
            return new ProgressAware[] { waveformController, artworkController };
        }
    }

}
