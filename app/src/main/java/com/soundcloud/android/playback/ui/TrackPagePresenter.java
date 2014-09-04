package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.playback.service.Playa.StateTransition;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;
import com.soundcloud.android.R;
import com.soundcloud.android.events.PlayableUpdatedEvent;
import com.soundcloud.android.image.ImageListener;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ui.progress.ProgressAware;
import com.soundcloud.android.playback.ui.progress.ScrubController;
import com.soundcloud.android.playback.ui.view.PlayerTrackArtworkView;
import com.soundcloud.android.playback.ui.view.TimestampView;
import com.soundcloud.android.playback.ui.view.WaveformView;
import com.soundcloud.android.playback.ui.view.WaveformViewController;
import com.soundcloud.android.tracks.TrackUrn;
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

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

class TrackPagePresenter implements PlayerPagePresenter, View.OnClickListener {

    private static final int SCRUB_TRANSITION_ALPHA_DURATION = 100;

    private final WaveformOperations waveformOperations;
    private final TrackPageListener listener;
    private final WaveformViewController.Factory waveformControllerFactory;
    private final PlayerArtworkController.Factory artworkControllerFactory;
    private final PlayerOverlayController.Factory playerOverlayControllerFactory;
    private final TrackMenuController.Factory trackMenuControllerFactory;
    private final SlideAnimationHelper helper = new SlideAnimationHelper();

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
                listener.onFooterTogglePlay();
                break;
            case R.id.player_play:
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
            case R.id.track_page_like:
                final TrackUrn trackUrn = (TrackUrn) view.getTag();
                updateLikeStatus(view, trackUrn);
                break;
            case R.id.profile_link:
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
    public View createItemView(ViewGroup container, SkipListener skipListener) {
        final View trackView = LayoutInflater.from(container.getContext()).inflate(R.layout.player_track_page, container, false);
        setupHolder(trackView);
        setupSkipListener(trackView, skipListener);
        return trackView;
    }

    @Override
    public void bindItemView(View view, PropertySet track, boolean isCurrentTrack) {
        bindItemView(view, new PlayerTrack(track), isCurrentTrack);
    }

    private void bindItemView(View trackView, PlayerTrack track, boolean isCurrentTrack) {
        final TrackPageHolder holder = getViewHolder(trackView);
        holder.title.setText(track.getTitle());

        holder.user.setText(track.getUserName());
        holder.profileLink.setTag(track.getUserUrn());

        holder.artworkController.loadArtwork(track.getUrn(), isCurrentTrack);
        holder.waveformController.displayWaveform(waveformOperations.waveformDataFor(track.getUrn(), track.getWaveformUrl()));
        holder.timestamp.setInitialProgress(track.getDuration());
        holder.waveformController.setDuration(track.getDuration());
        holder.menuController.setTrack(track);

        setLikeCount(holder, track.getLikeCount());
        holder.likeToggle.setChecked(track.isUserLike());
        holder.likeToggle.setTag(track.getUrn());

        holder.footerUser.setText(track.getUserName());
        holder.footerTitle.setText(track.getTitle());

        holder.timestamp.setVisibility(View.VISIBLE);

        setClickListener(this, holder.onClickViews);
    }

    public View clearItemView(View view) {
        final TrackPageHolder holder = getViewHolder(view);
        holder.user.setText(ScTextUtils.EMPTY_STRING);
        holder.title.setText(ScTextUtils.EMPTY_STRING);
        holder.likeToggle.setChecked(false);
        holder.likeToggle.setEnabled(true);

        holder.artworkController.reset();
        holder.waveformController.reset();

        holder.footerUser.setText(ScTextUtils.EMPTY_STRING);
        holder.footerTitle.setText(ScTextUtils.EMPTY_STRING);

        holder.timestamp.setVisibility(View.GONE);
        return view;
    }

    @Override
    public void setPlayState(View trackPage, StateTransition stateTransition, boolean isCurrentTrack) {
        final TrackPageHolder holder = getViewHolder(trackPage);
        final boolean playSessionIsActive = stateTransition.playSessionIsActive();
        holder.playControlsHolder.setVisibility(playSessionIsActive ? View.GONE : View.VISIBLE);
        holder.footerPlayToggle.setChecked(playSessionIsActive);
        setWaveformPlayState(holder, stateTransition, isCurrentTrack);
        setViewPlayState(holder, stateTransition, isCurrentTrack);

        holder.timestamp.setBufferingMode(isCurrentTrack && stateTransition.isBuffering());

        if (stateTransition.playSessionIsActive() && !isCurrentTrack) {
            setProgress(trackPage, PlaybackProgress.empty());
        }
    }

    @Override
    public void onPlayableUpdated(View trackPage, PlayableUpdatedEvent playableUpdatedEvent) {
        final TrackPageHolder holder = getViewHolder(trackPage);
        final PropertySet changeSet = playableUpdatedEvent.getChangeSet();

        if (changeSet.contains(PlayableProperty.IS_LIKED)) {
            holder.likeToggle.setChecked(changeSet.get(PlayableProperty.IS_LIKED));
        }
        if (changeSet.contains(PlayableProperty.LIKES_COUNT)) {
            setLikeCount(holder, changeSet.get(PlayableProperty.LIKES_COUNT));
        }

        if (changeSet.contains(PlayableProperty.IS_REPOSTED)) {
            final boolean isReposted = changeSet.get(PlayableProperty.IS_REPOSTED);
            holder.menuController.setIsUserRepost(isReposted);

            if (playableUpdatedEvent.isFromRepost()) {
                showRepostToast(trackPage.getContext(), isReposted);
            }
        }
    }

    @Override
    public void onPositionSet(View trackPage, int position, int size) {
        final TrackPageHolder holder = getViewHolder(trackPage);
        if (holder.hasNextButton()) {
            holder.nextButton.setVisibility(position == size - 1 ? View.INVISIBLE : View.VISIBLE);
        }
        if (holder.hasPreviousButton()) {
            holder.previousButton.setVisibility(position == 0 ? View.INVISIBLE : View.VISIBLE);
        }
    }

    private void showRepostToast(final Context context, final boolean isReposted) {
        Toast.makeText(context, isReposted
                ? R.string.reposted_to_followers
                : R.string.unposted_to_followers, Toast.LENGTH_SHORT).show();
    }

    private void updateLikeStatus(View view, TrackUrn trackUrn) {
        boolean isLike = ((Checkable) view).isChecked();
        listener.onToggleLike(isLike, trackUrn);
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
                holder.artworkController.showIdleState();
            }
        } else {
            holder.artworkController.showIdleState();
        }

        setOverlayPlayState(holder, state);
        setTextBackgrounds(holder, state.playSessionIsActive());
    }

    private void setOverlayPlayState(TrackPageHolder holder, StateTransition state) {
        for (PlayerOverlayController playerOverlayController : holder.playerOverlayControllers) {
            if (state.playSessionIsActive()) {
                playerOverlayController.showPlayingState();
            } else {
                playerOverlayController.showIdleState();
            }
        }
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
        for (ProgressAware view : getViewHolder(trackPage).progressAwares) {
            view.setProgress(progress);
        }
    }

    @Override
    public void setCollapsed(View trackView) {
        onPlayerSlide(trackView, 0);
        getViewHolder(trackView).waveformController.setCollapsed();
    }

    @Override
    public void setExpanded(View trackView) {
        onPlayerSlide(trackView, 1);
        getViewHolder(trackView).waveformController.setExpanded();
    }

    @Override
    public void onPlayerSlide(View trackView, float slideOffset) {
        TrackPageHolder holder = getViewHolder(trackView);
        helper.configureViewsFromSlide(slideOffset, holder.footer, holder.fullScreenViews, holder.playerOverlayControllers);
        holder.waveformController.onPlayerSlide(slideOffset);
    }

    public boolean accept(View view) {
        return view.getTag() instanceof TrackPageHolder;
    }

    private void setClickListener(View.OnClickListener listener, Iterable<View> views) {
        for (View v : views) {
            v.setOnClickListener(listener);
        }
    }

    private void setupSkipListener(View trackView, final SkipListener skipListener) {
        TrackPageHolder holder = getViewHolder(trackView);

        final View.OnClickListener nextListener = getOnNextListener(skipListener);
        final View.OnClickListener previousListener = getOnPreviousListener(skipListener);
        if (holder.hasNextButton()) {
            holder.nextButton.setOnClickListener(nextListener);
        }
        if (holder.hasPreviousButton()) {
            holder.previousButton.setOnClickListener(previousListener);
        }
    }

    private View.OnClickListener getOnPreviousListener(final SkipListener skipListener) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                skipListener.onPrevious();
            }
        };
    }

    private View.OnClickListener getOnNextListener(final SkipListener skipListener) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                skipListener.onNext();
            }
        };
    }

    private ScrubController.OnScrubListener createScrubViewAnimations(final TrackPageHolder holder) {
        return new ScrubController.OnScrubListener() {
            @Override
            public void scrubStateChanged(int newScrubState) {
                listener.onScrub(newScrubState);
                for (View v : holder.hideOnScrubViews) {
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

    private TrackPageHolder getViewHolder(View trackView) {
        return (TrackPageHolder) trackView.getTag();
    }

    private void setupHolder(View trackView) {
        TrackPageHolder holder = new TrackPageHolder();
        holder.title = (JaggedTextView) trackView.findViewById(R.id.track_page_title);
        holder.user = (JaggedTextView) trackView.findViewById(R.id.track_page_user);
        holder.artworkView = (PlayerTrackArtworkView) trackView.findViewById(R.id.track_page_artwork);
        holder.timestamp = (TimestampView) trackView.findViewById(R.id.timestamp);
        holder.likeToggle = (ToggleButton) trackView.findViewById(R.id.track_page_like);
        holder.more = trackView.findViewById(R.id.track_page_more);
        holder.close = trackView.findViewById(R.id.player_close);
        holder.bottomClose = trackView.findViewById(R.id.player_bottom_close);
        holder.nextButton = trackView.findViewById(R.id.player_next);
        holder.previousButton = trackView.findViewById(R.id.player_previous);
        holder.playButton = trackView.findViewById(R.id.player_play);
        holder.profileLink = trackView.findViewById(R.id.profile_link);

        holder.footer = trackView.findViewById(R.id.footer_controls);
        holder.footerPlayToggle = (ToggleButton) trackView.findViewById(R.id.footer_toggle);
        holder.footerTitle = (TextView) trackView.findViewById(R.id.footer_title);
        holder.footerUser = (TextView) trackView.findViewById(R.id.footer_user);

        final WaveformView waveform = (WaveformView) trackView.findViewById(R.id.track_page_waveform);
        holder.waveformController = waveformControllerFactory.create(waveform);

        holder.playerOverlayControllers = new PlayerOverlayController[] {
                playerOverlayControllerFactory.create(holder.artworkView.findViewById(R.id.artwork_overlay_dark)),
                playerOverlayControllerFactory.create(holder.artworkView.findViewById(R.id.artwork_overlay_image))
        };

        holder.artworkController = artworkControllerFactory.create(holder.artworkView);
        holder.waveformController.addScrubListener(holder.artworkController);
        holder.waveformController.addScrubListener(holder.timestamp);
        holder.waveformController.addScrubListener(createScrubViewAnimations(holder));
        holder.menuController = trackMenuControllerFactory.create(holder.more);
        holder.playControlsHolder = trackView.findViewById(R.id.play_controls);
        holder.closeIndicator = trackView.findViewById(R.id.player_close_indicator);

        for (PlayerOverlayController playerOverlayController : holder.playerOverlayControllers) {
            holder.waveformController.addScrubListener(playerOverlayController);
        }

        holder.populateViewSets();
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
        ImageListener artworkListener;
        PlayerOverlayController[] playerOverlayControllers;
        ToggleButton likeToggle;
        View more;
        View close;
        View bottomClose;
        View nextButton;
        View previousButton;
        View playButton;
        View playControlsHolder;
        View closeIndicator;
        View profileLink;

        // Footer player
        View footer;
        ToggleButton footerPlayToggle;
        TextView footerTitle;
        TextView footerUser;

        // View sets
        Iterable<View> fullScreenViews;
        Iterable<View> hideOnScrubViews;
        Iterable<View> onClickViews;
        Iterable<ProgressAware> progressAwares;

        private Predicate<View> presentInConfig = new Predicate<View>() {
            @Override
            public boolean apply(@Nullable View v) {
                return v != null;
            }
        };

        public void populateViewSets() {
            List<View> hideViews = Arrays.asList(title, user, closeIndicator, nextButton, previousButton, playButton, bottomClose);
            List<View> clickViews = Arrays.asList(artworkView, close, bottomClose, playButton, footer, footerPlayToggle, likeToggle, profileLink);

            fullScreenViews = Arrays.asList(title, user, close, timestamp);
            hideOnScrubViews = Iterables.filter(hideViews, presentInConfig);
            onClickViews = Iterables.filter(clickViews, presentInConfig);
            progressAwares = Lists.<ProgressAware>newArrayList(waveformController, artworkController, timestamp);
        }

        private boolean hasNextButton() {
            return nextButton != null;
        }

        private boolean hasPreviousButton() {
            return previousButton != null;
        }
    }

}
