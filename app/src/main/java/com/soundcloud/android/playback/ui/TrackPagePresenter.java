package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.playback.service.Playa.StateTransition;

import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;
import com.soundcloud.android.R;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ui.progress.ProgressAware;
import com.soundcloud.android.playback.ui.progress.ScrubController;
import com.soundcloud.android.playback.ui.view.PlayerTrackArtworkView;
import com.soundcloud.android.playback.ui.view.TimestampView;
import com.soundcloud.android.playback.ui.view.WaveformView;
import com.soundcloud.android.playback.ui.view.WaveformViewController;
import com.soundcloud.android.users.UserUrn;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.JaggedTextView;
import com.soundcloud.android.waveform.WaveformOperations;
import com.soundcloud.propeller.PropertySet;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import javax.inject.Inject;

class TrackPagePresenter implements PagePresenter, View.OnClickListener {

    private static final int SCRUB_TRANSITION_ALPHA_DURATION = 100;

    private final WaveformOperations waveformOperations;
    private final TrackPageListener listener;
    private final WaveformViewController.Factory waveformControllerFactory;
    private final PlayerArtworkController.Factory artworkControllerFactory;
    private final PlayerOverlayController.Factory playerOverlayControllerFactory;
    private final TrackMenuController.Factory trackMenuControllerFactory;

    @Inject
    public TrackPagePresenter(WaveformOperations waveformOperations, TrackPageListener listener,
                              WaveformViewController.Factory waveformControllerFactory,
                              PlayerArtworkController.Factory artworkControllerFactory,
                              PlayerOverlayController.Factory playerOverlayControllerFactory,
                              TrackMenuController.Factory trackMenuControllerFactory) {
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
            case R.id.track_page_user:
                final Context activityContext = view.getContext();
                final UserUrn userUrn = (UserUrn) view.getTag();
                listener.onGotoUser(activityContext, userUrn);
                break;
            default:
                throw new IllegalArgumentException("Unexpected view ID: "
                        + view.getContext().getResources().getResourceName(view.getId()));
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
        holder.title.setText(track.getTitle());

        holder.user.setText(track.getUserName());
        holder.user.setTag(track.getUserUrn());

        holder.artworkController.loadArtwork(track.getUrn());
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
        holder.artworkController.clear();

        holder.footerUser.setText(ScTextUtils.EMPTY_STRING);
        holder.footerTitle.setText(ScTextUtils.EMPTY_STRING);
        return view;
    }

    @Override
    public void setPlayState(View trackPage, StateTransition stateTransition, boolean isCurrentTrack) {
        final TrackPageHolder holder = getViewHolder(trackPage);
        final boolean playSessionIsActive = stateTransition.playSessionIsActive();
        setVisibility(!playSessionIsActive, holder.playControlsHolder);
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
            boolean isReposted = changeSet.get(PlayableProperty.IS_REPOSTED);
            holder.menuController.setIsUserRepost(isReposted);
            showRepostToast(trackPage.getContext(), isReposted);
        }
    }

    private void showRepostToast(final Context context, final boolean isReposted) {
        if (isReposted) {
            Toast.makeText(context, R.string.reposted_to_followers, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, R.string.unposted_to_followers, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateLikeStatus(View view) {
        boolean isLike = ((Checkable) view).isChecked();
        listener.onToggleLike(isLike);
    }

    private void setLikeCount(TrackPageHolder holder, int count) {
        holder.likeToggle.setText(ScTextUtils.formatLargeNumber(count));
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
        for (ProgressAware view : getViewHolder(trackPage).getProgressAwareItems()) {
            view.setProgress(progress);
        }
    }

    public void setExpanded(View trackView, boolean isPlaying) {
        TrackPageHolder holder = getViewHolder(trackView);
        holder.footer.setVisibility(View.GONE);
        holder.waveformController.setWaveformVisibility(isPlaying);
        holder.playerOverlayController.setExpandedAndUpdate();
        holder.waveformController.setWaveformVisibility(true);
        setVisibility(true, holder.getFullScreenViews());
    }

    public void setCollapsed(View trackView) {
        TrackPageHolder holder = getViewHolder(trackView);
        holder.footer.setVisibility(View.VISIBLE);
        holder.playerOverlayController.setCollapsedAndUpdate();
        holder.waveformController.setWaveformVisibility(false);
        setVisibility(false, holder.getFullScreenViews());
    }

    public boolean accept(View view) {
        return view.getTag() instanceof TrackPageHolder;
    }

    private void setVisibility(boolean visible, View... views) {
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
        holder.waveformController.addScrubListener(holder.playerOverlayController);
        holder.waveformController.addScrubListener(createScrubViewAnimations(holder));
        holder.menuController = trackMenuControllerFactory.create(holder.more);
        holder.playControlsHolder = trackView.findViewById(R.id.animateThis);
        holder.closeIndicator = trackView.findViewById(R.id.player_close_indicator);

        trackView.setTag(holder);
    }

    private ScrubController.OnScrubListener createScrubViewAnimations(final TrackPageHolder holder) {
        return new ScrubController.OnScrubListener() {
            @Override
            public void scrubStateChanged(int newScrubState) {
                for (View v : holder.getHideOnScrubViews()) {
                    ObjectAnimator animator = ObjectAnimator.ofFloat(v, "alpha", ViewHelper.getAlpha(v),
                            newScrubState == ScrubController.SCRUB_STATE_SCRUBBING ? 0 : 1);
                    animator.setDuration(SCRUB_TRANSITION_ALPHA_DURATION);
                    animator.start();
                }
            }

            @Override
            public void displayScrubPosition(float scrubPosition) {
                // no-op
            }
        };
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

        View playControlsHolder;
        View closeIndicator;

        public View[] getOnClickViews() {
            return new View[] { artworkView, close, bottomClose, nextTouch, previousTouch,
                    playButton, footer, footerPlayToggle, likeToggle, user };
        }

        public View[] getFullScreenViews() {
            return new View[] { title, user, close };
        }

        public ProgressAware[] getProgressAwareItems() {
            return new ProgressAware[] { waveformController, artworkController, timestamp };
        }

        public View[] getHideOnScrubViews() { return new View[] { title, user, closeIndicator, nextButton, previousButton, playButton }; }
    }

}
