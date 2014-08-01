package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.playback.service.Playa.StateTransition;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ui.progress.ProgressAware;
import com.soundcloud.android.playback.ui.progress.ScrubController;
import com.soundcloud.android.playback.ui.view.PlayerTrackArtworkView;
import com.soundcloud.android.playback.ui.view.TimestampView;
import com.soundcloud.android.playback.ui.view.WaveformView;
import com.soundcloud.android.playback.ui.view.WaveformViewController;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.JaggedTextView;
import com.soundcloud.android.waveform.WaveformOperations;
import com.soundcloud.propeller.PropertySet;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.TextView;
import android.widget.ToggleButton;

import javax.inject.Inject;

class TrackPagePresenter implements PagePresenter, View.OnClickListener {

    private final Resources resources;
    private final ImageOperations imageOperations;
    private final WaveformOperations waveformOperations;
    private final TrackPageListener listener;
    private final WaveformViewController.Factory waveformControllerFactory;
    private final PlayerArtworkController.Factory artworkControllerFactory;
    private final PlayerOverlayController.Factory playerOverlayControllerFactory;
    private final TrackMenuController.Factory trackMenuControllerFactory;

    @Inject
    public TrackPagePresenter(Resources resources, ImageOperations imageOperations,
                              WaveformOperations waveformOperations, TrackPageListener listener,
                              WaveformViewController.Factory waveformControllerFactory,
                              PlayerArtworkController.Factory artworkControllerFactory,
                              PlayerOverlayController.Factory playerOverlayControllerFactory,
                              TrackMenuController.Factory trackMenuControllerFactory) {
        this.resources = resources;
        this.imageOperations = imageOperations;
        this.waveformOperations = waveformOperations;
        this.listener = listener;
        this.waveformControllerFactory = waveformControllerFactory;
        this.artworkControllerFactory = artworkControllerFactory;
        this.playerOverlayControllerFactory = playerOverlayControllerFactory;
        this.trackMenuControllerFactory = trackMenuControllerFactory;
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
            case R.id.track_page_like:
                updateLikeStatus(view);
                break;
            default:
                throw new IllegalArgumentException("Unexpected view ID: "
                        + resources.getResourceName(view.getId()));
        }
    }

    @Override
    public View createItemView(ViewGroup container) {
        final View trackView = LayoutInflater.from(container.getContext()).inflate(R.layout.player_track_page, container, false);
        setupHolder(trackView);
        return trackView;
    }

    @Override
    public void bindItemView(View view, PropertySet track) {
        bindItemView(view, new PlayerTrack(track));
    }

    private void bindItemView(View trackView, PlayerTrack track) {
        final TrackPageHolder holder = getViewHolder(trackView);
        holder.user.setText(track.getUserName());
        holder.title.setText(track.getTitle());
        imageOperations.displayInVisualPlayer(track.getUrn(), ApiImageSize.getFullImageSize(resources),
                holder.artworkController.getImageView(), holder.artworkController.getImageListener());
        holder.waveformController.displayWaveform(waveformOperations.waveformDataFor(track.getUrn(), track.getWaveformUrl()));
        holder.timestamp.setInitialProgress(track.getDuration());
        holder.waveformController.setDuration(track.getDuration());
        holder.menuController.setTrack(track);

        setLikeCount(holder, track.getLikeCount());
        holder.likeToggle.setChecked(track.isUserLike());

        holder.footerUser.setText(track.getUserName());
        holder.footerTitle.setText(track.getTitle());

        setClickListener(holder.getOnClickViews(), this);
    }

    public View clearItemView(View view) {
        final TrackPageHolder holder = getViewHolder(view);
        holder.user.setText(ScTextUtils.EMPTY_STRING);
        holder.title.setText(ScTextUtils.EMPTY_STRING);
        holder.likeToggle.setChecked(false);
        holder.likeToggle.setEnabled(true);

        holder.waveformController.reset();
        holder.artworkController.getImageView().setImageDrawable(null);

        holder.footerUser.setText(ScTextUtils.EMPTY_STRING);
        holder.footerTitle.setText(ScTextUtils.EMPTY_STRING);
        return view;
    }

    @Override
    public void setPlayState(View trackPage, StateTransition stateTransition, boolean isCurrentTrack) {
        final TrackPageHolder holder = getViewHolder(trackPage);
        final boolean playSessionIsActive = stateTransition.playSessionIsActive();

        setVisibility(holder.getPlayControls(), !playSessionIsActive);
        holder.footerPlayToggle.setChecked(playSessionIsActive && isCurrentTrack);
        setWaveformPlayState(holder, stateTransition, isCurrentTrack);
        setViewPlayState(holder, stateTransition, isCurrentTrack);
    }

    @Override
    public void updateAssociations(View trackPage, PropertySet changeSet) {
        final TrackPageHolder holder = getViewHolder(trackPage);
        if (changeSet.contains(PlayableProperty.IS_LIKED)) {
            holder.likeToggle.setChecked(changeSet.get(PlayableProperty.IS_LIKED));
        }
        if (changeSet.contains(PlayableProperty.LIKES_COUNT)) {
            setLikeCount(holder, changeSet.get(PlayableProperty.LIKES_COUNT));
        }
        if (changeSet.contains(PlayableProperty.IS_REPOSTED)) {
            holder.menuController.setIsUserRepost(changeSet.get(PlayableProperty.IS_REPOSTED));
        }
    }

    private void updateLikeStatus(View view) {
        boolean isLike = ((Checkable) view).isChecked();
        listener.onToggleLike(isLike);
    }

    private void setLikeCount(TrackPageHolder holder, int count) {
        holder.likeToggle.setText(ScTextUtils.shortenLargeNumber(count));
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

    private void setViewPlayState(TrackPageHolder holder, StateTransition state, boolean isCurrentTrack) {
        if (state.playSessionIsActive()) {
            if (isCurrentTrack && state.isPlayerPlaying()) {
                holder.artworkController.showPlayingState(state.getProgress());
            } else {
                holder.artworkController.showSessionActiveState();
            }
        } else {
            holder.artworkController.showIdleState();
        }
        holder.playerOverlayController.update();
        setTextBackgrounds(holder, state.playSessionIsActive());
    }

    private void setTextBackgrounds(TrackPageHolder holder, boolean visible) {
        holder.title.showBackground(visible);
        holder.user.showBackground(visible);
        holder.timestamp.showBackground(visible);
    }

    public void onPageChange(View trackView) {
        TrackPageHolder holder = getViewHolder(trackView);
        holder.waveformController.scrubStateChanged(ScrubController.SCRUB_STATE_NONE);
        holder.menuController.dismiss();
    }

    @Override
    public void setProgress(View trackPage, PlaybackProgress progress) {
        for (ProgressAware view : getViewHolder(trackPage).getProgressAwareViews()) {
            view.setProgress(progress);
        }
    }

    public void setExpanded(View trackView, boolean isPlaying) {
        TrackPageHolder holder = getViewHolder(trackView);
        holder.footer.setVisibility(View.GONE);
        holder.waveformController.setWaveformVisibility(isPlaying);
        holder.playerOverlayController.setExpandedAndUpdate();
        holder.waveformController.setWaveformVisibility(true);
        setVisibility(holder.getFullScreenViews(), true);
    }

    public void setCollapsed(View trackView) {
        TrackPageHolder holder = getViewHolder(trackView);
        holder.footer.setVisibility(View.VISIBLE);
        holder.playerOverlayController.setCollapsedAndUpdate();
        holder.waveformController.setWaveformVisibility(false);
        setVisibility(holder.getFullScreenViews(), false);
    }

    public boolean accept(View view) {
        return view.getTag() instanceof TrackPageHolder;
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
        holder.artworkView = (PlayerTrackArtworkView) trackView.findViewById(R.id.track_page_artwork);
        holder.artworkOverlay = holder.artworkView.findViewById(R.id.artwork_overlay);
        holder.timestamp = (TimestampView) trackView.findViewById(R.id.timestamp);
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
        holder.playerOverlayController = playerOverlayControllerFactory.create(holder.artworkOverlay);
        holder.waveformController.addScrubListener(holder.artworkController);
        holder.waveformController.addScrubListener(holder.timestamp);

        holder.menuController = trackMenuControllerFactory.create(holder.more);

        trackView.setTag(holder);
    }

    static class TrackPageHolder {
        // Expanded player
        JaggedTextView title;
        JaggedTextView user;
        WaveformViewController waveformController;
        TrackMenuController menuController;
        TimestampView timestamp;
        PlayerTrackArtworkView artworkView;
        PlayerArtworkController artworkController;
        PlayerOverlayController playerOverlayController;
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
        View artworkOverlay;

        public View[] getOnClickViews() {
            return new View[] { artworkView, close, bottomClose, nextTouch, previousTouch, playButton, footer,
                    footerPlayToggle, likeToggle };
        }

        public View[] getFullScreenViews() {
            return new View[] { title, user, close };
        }

        public View[] getPlayControls() {
            return new View[] { nextButton, previousButton, playButton };
        }

        public ProgressAware[] getProgressAwareViews() {
            return new ProgressAware[] { waveformController, artworkController, timestamp };
        }
    }

}
