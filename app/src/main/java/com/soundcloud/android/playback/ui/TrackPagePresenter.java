package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.tracks.TieredTracks.isFullHighTierTrack;
import static com.soundcloud.android.tracks.TieredTracks.isHighTierPreview;
import static java.util.Collections.singletonList;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdOverlayController;
import com.soundcloud.android.ads.AdOverlayControllerFactory;
import com.soundcloud.android.ads.OverlayAdData;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.cast.CastPlayerStripController;
import com.soundcloud.android.cast.CastPlayerStripControllerFactory;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.experiments.PlayQueueConfiguration;
import com.soundcloud.android.configuration.experiments.PlayerUpsellCopyExperiment;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayKey;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayPresenter;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.payments.PlayerUpsellImpressionController;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayStateEvent;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.playback.ui.progress.ProgressAware;
import com.soundcloud.android.playback.ui.progress.ScrubController;
import com.soundcloud.android.playback.ui.view.PlayerStripView;
import com.soundcloud.android.playback.ui.view.PlayerTrackArtworkView;
import com.soundcloud.android.playback.ui.view.PlayerUpsellView;
import com.soundcloud.android.playback.ui.view.TimestampView;
import com.soundcloud.android.playback.ui.view.WaveformView;
import com.soundcloud.android.playback.ui.view.WaveformViewController;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.android.util.AnimUtils;
import com.soundcloud.android.view.DefaultAnimationListener;
import com.soundcloud.android.view.JaggedTextView;
import com.soundcloud.android.waveform.WaveformOperations;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import org.jetbrains.annotations.Nullable;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.MediaRouteButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class TrackPagePresenter implements PlayerPagePresenter<PlayerTrackState>, View.OnClickListener {

    private static final int SCRUB_TRANSITION_ALPHA_DURATION = 100;
    private static final long ANIMATION_DURATION = 600;

    private final WaveformOperations waveformOperations;
    private final FeatureOperations featureOperations;
    private final TrackPageListener listener;
    private final LikeButtonPresenter likeButtonPresenter;
    private final IntroductoryOverlayPresenter introductoryOverlayPresenter;
    private final WaveformViewController.Factory waveformControllerFactory;
    private final PlayerArtworkController.Factory artworkControllerFactory;
    private final PlayerOverlayController.Factory playerOverlayControllerFactory;
    private final TrackPageMenuController.Factory trackMenuControllerFactory;
    private final CastPlayerStripControllerFactory castPlayerStripControllerFactory;
    private final AdOverlayControllerFactory adOverlayControllerFactory;
    private final ErrorViewControllerFactory errorControllerFactory;
    private final EmptyViewControllerFactory emptyControllerFactory;
    private final CastConnectionHelper castConnectionHelper;
    private final Resources resources;
    private final PlayerUpsellImpressionController upsellImpressionController;
    private final PlayerUpsellCopyExperiment upsellCopyExperiment;
    private final PlayQueueConfiguration playQueueConfiguration;

    private final SlideAnimationHelper slideHelper = new SlideAnimationHelper();

    @Inject
    TrackPagePresenter(WaveformOperations waveformOperations,
                       FeatureOperations featureOperations,
                       TrackPageListener listener,
                       LikeButtonPresenter likeButtonPresenter,
                       IntroductoryOverlayPresenter introductoryOverlayPresenter,
                       WaveformViewController.Factory waveformControllerFactory,
                       PlayerArtworkController.Factory artworkControllerFactory,
                       PlayerOverlayController.Factory playerOverlayControllerFactory,
                       TrackPageMenuController.Factory trackMenuControllerFactory,
                       CastPlayerStripControllerFactory castPlayerStripControllerFactory,
                       AdOverlayControllerFactory adOverlayControllerFactory,
                       ErrorViewControllerFactory errorControllerFactory,
                       EmptyViewControllerFactory emptyControllerFactory,
                       CastConnectionHelper castConnectionHelper,
                       Resources resources,
                       PlayerUpsellImpressionController upsellImpressionController,
                       PlayerUpsellCopyExperiment upsellCopyExperiment,
                       PlayQueueConfiguration playQueueConfiguration) {
        this.waveformOperations = waveformOperations;
        this.featureOperations = featureOperations;
        this.listener = listener;
        this.likeButtonPresenter = likeButtonPresenter;
        this.introductoryOverlayPresenter = introductoryOverlayPresenter;
        this.waveformControllerFactory = waveformControllerFactory;
        this.artworkControllerFactory = artworkControllerFactory;
        this.playerOverlayControllerFactory = playerOverlayControllerFactory;
        this.trackMenuControllerFactory = trackMenuControllerFactory;
        this.castPlayerStripControllerFactory = castPlayerStripControllerFactory;
        this.adOverlayControllerFactory = adOverlayControllerFactory;
        this.errorControllerFactory = errorControllerFactory;
        this.emptyControllerFactory = emptyControllerFactory;
        this.castConnectionHelper = castConnectionHelper;
        this.resources = resources;
        this.upsellImpressionController = upsellImpressionController;
        this.upsellCopyExperiment = upsellCopyExperiment;
        this.playQueueConfiguration = playQueueConfiguration;
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
            case R.id.player_close_indicator:
            case R.id.player_bottom_close:
                listener.onPlayerClose();
                break;
            case R.id.profile_link:
                final Context activityContext = view.getContext();
                final Urn userUrn = (Urn) view.getTag();
                listener.onGotoUser(activityContext, userUrn);
                break;
            case R.id.upsell_button:
                listener.onUpsell(view.getContext(), (Urn) view.getTag());
                break;
            case R.id.play_queue_button:
                listener.onPlayQueue();
                break;
            default:
                throw new IllegalArgumentException("Unexpected view ID: "
                                                           + resources.getResourceName(view.getId()));
        }
    }

    @Override
    public View createItemView(ViewGroup container, SkipListener skipListener) {
        final View trackView = LayoutInflater.from(container.getContext())
                                             .inflate(R.layout.player_track_page, container, false);
        setupHolder(trackView);
        setupSkipListener(trackView, skipListener);
        return trackView;
    }

    @Override
    public void bindItemView(View trackView, PlayerTrackState trackState) {
        final String title = trackState.getTitle();
        final String userName = trackState.getUserName();
        final Urn userUrn = trackState.getUserUrn();
        final Urn urn = trackState.getUrn();
        final long fullDuration = trackState.getFullDuration();
        final long playableDuration = trackState.getPlayableDuration();

        final TrackPageHolder holder = getViewHolder(trackView);
        holder.title.setText(title);
        holder.user.setText(userName);
        holder.profileLink.setTag(userUrn);
        bindStationsContext(trackState, holder);

        updatePlayQueueButton(trackView);

        holder.artworkController.loadArtwork(trackState, false, trackState.getViewVisibilityProvider());
        holder.timestamp.setInitialProgress(playableDuration, fullDuration);
        holder.menuController.setTrack(trackState);
        holder.waveformController.setWaveform(waveformOperations.waveformDataFor(urn,
                                                                                 trackState.getWaveformUrl()),
                                              trackState.isForeground());

        holder.artworkController.setFullDuration(fullDuration);
        holder.waveformController.setDurations(playableDuration, fullDuration);

        likeButtonPresenter.setLikeCount(holder.likeToggle, trackState.getLikeCount(),
                                         R.drawable.ic_player_liked, R.drawable.ic_player_like);

        holder.likeToggle.setChecked(trackState.isUserLike());
        holder.likeToggle.setTag(urn);
        holder.shareButton.setTag(urn);

        holder.footerUser.setText(userName);
        holder.footerTitle.setText(title);

        holder.upsellView.getUpsellButton().setTag(urn);
        configurePlayerStates(trackState, urn, holder);

        setClickListener(this, holder.onClickViews);
        setCastDeviceName(trackView, castConnectionHelper.getDeviceName(), false);
    }

    private void configurePlayerStates(PlayerTrackState trackState, Urn urn, TrackPageHolder holder) {
        configureHighTierStates(trackState, holder, featureOperations);
        setPlayButtonsEnabled(holder, !trackState.isEmpty() && !trackState.isBlocked());
        configureEmptyState(holder, trackState);
        configureBlockedState(holder, trackState);

        holder.errorViewController.setUrn(urn);
    }

    @Override
    public void showIntroductoryOverlayForPlayQueue(View trackView) {
        final View playQueueButton = getViewHolder(trackView).playQueueButton;
        introductoryOverlayPresenter.showIfNeeded(IntroductoryOverlayKey.PLAY_QUEUE,
                                                  playQueueButton, resources.getString(R.string.play_queue_introductory_overlay_title),
                                                  resources.getString(R.string.play_queue_introductory_overlay_description));
    }

    @Override
    public void updatePlayQueueButton(View view) {
        final TrackPageHolder holder = getViewHolder(view);

        if (playQueueConfiguration.isEnabled()) {
            holder.playQueueButton.setVisibility(View.VISIBLE);
        } else {
            holder.playQueueButton.setVisibility(View.GONE);
        }
    }

    private void configureHighTierStates(PlayerTrackState trackState,
                                         TrackPageHolder holder,
                                         FeatureOperations featureOperations) {
        if (isHighTierPreview(trackState)) {
            holder.topStrip.setBackgroundResource(R.drawable.go_gradient);
            configureUpsell(holder, featureOperations);
        } else if (isFullHighTierTrack(trackState)) {
            holder.topStrip.setBackgroundResource(R.drawable.go_gradient);
        } else {
            holder.topStrip.setBackgroundResource(R.color.ak_sc_orange);
            holder.upsellView.setVisibility(View.GONE);
        }
    }

    private void configureUpsell(TrackPageHolder holder, FeatureOperations featureOperations) {
        if (featureOperations.upsellHighTier()) {
            holder.upsellView.showUpsell(upsellCopyExperiment.getUpsellCtaId(), getUpsellButtonText());
            holder.timestamp.setPreview(true);
        } else {
            holder.upsellView.setVisibility(View.GONE);
            holder.timestamp.setPreview(false);
        }
    }

    private int getUpsellButtonText() {
        return featureOperations.isHighTierTrialEligible() ? R.string.playback_upsell_button_trial : R.string.playback_upsell_button;
    }

    private void configureEmptyState(TrackPageHolder holder, PlayerTrackState trackState) {
        final boolean empty = trackState.isEmpty();

        if (empty) {
            holder.emptyViewController.show();
        }
    }

    private void configureBlockedState(TrackPageHolder holder, PlayerTrackState trackState) {
        final boolean blocked = trackState.isBlocked();
        holder.artworkView.setEnabled(!blocked);

        for (PlayerOverlayController playerOverlayController : holder.playerOverlayControllers) {
            playerOverlayController.setBlocked(blocked);
        }

        if (blocked) {
            holder.errorViewController.showError(ErrorViewController.ErrorState.BLOCKED);
        } else {
            if (!holder.errorViewController.isShowingError()) {
                holder.timestamp.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setPlayButtonsEnabled(TrackPageHolder holder, boolean enabled) {
        if (holder.playButton != null) {
            holder.playButton.setEnabled(enabled);
        }
        if (holder.footerPlayToggle != null) {
            holder.footerPlayToggle.setEnabled(enabled);
        }
    }

    private void bindStationsContext(PlayerTrackState trackState, TrackPageHolder holder) {
        final Optional<StationRecord> station = trackState.getStation();

        if (station.isPresent()) {
            holder.trackContext.setVisibility(View.VISIBLE);
            holder.trackContext.setText(station.get().getTitle());
        } else {
            holder.trackContext.setVisibility(View.GONE);
        }
    }

    @Override
    public void setCastDeviceName(View view, String deviceName, boolean animate) {
        getViewHolder(view).castPlayerStripController.update(animate);
    }

    @Override
    public void onViewSelected(View view, PlayQueueItem playQueueItem, boolean isExpanded) {
        if (isExpanded && getViewHolder(view).upsellView.getVisibility() == View.VISIBLE) {
            upsellImpressionController.recordUpsellViewed(playQueueItem);
        }
    }

    @Override
    public void onItemAdded(View view) {
        final View footerQueueButton = getViewHolder(view).footerQueueButton;
        Animator alphaAnimator = ObjectAnimator.ofFloat(footerQueueButton, View.ALPHA, 0f, 1f, 0f);
        alphaAnimator.setDuration(ANIMATION_DURATION);
        alphaAnimator.addListener(new DefaultAnimationListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                footerQueueButton.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                footerQueueButton.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                footerQueueButton.setVisibility(View.GONE);
            }
        });
        alphaAnimator.setInterpolator(new FastOutSlowInInterpolator());
        alphaAnimator.start();
    }

    void setAdOverlay(View view, OverlayAdData adData) {
        getViewHolder(view).adOverlayController.initialize(adData);
    }

    public void clearAdOverlay(View view) {
        clearAdOverlay(getViewHolder(view));
    }

    private void clearAdOverlay(TrackPageHolder viewHolder) {
        viewHolder.adOverlayController.clear();
    }

    public View clearItemView(View view) {
        final TrackPageHolder holder = getViewHolder(view);
        holder.user.setText(Strings.EMPTY);
        holder.title.setText(Strings.EMPTY);

        holder.trackContext.setVisibility(View.GONE);

        holder.likeToggle.setChecked(false);
        holder.likeToggle.setEnabled(true);

        holder.artworkController.reset();
        holder.waveformController.reset();

        holder.footerUser.setText(Strings.EMPTY);
        holder.footerTitle.setText(Strings.EMPTY);

        holder.timestamp.setPreview(false);
        holder.timestamp.setVisibility(View.GONE);
        holder.errorViewController.hideError();
        holder.emptyViewController.hide();

        holder.upsellView.setVisibility(View.GONE);

        return view;
    }

    @Override
    public void setPlayState(View trackPage,
                             PlayStateEvent playStateEvent,
                             boolean isCurrentTrack,
                             boolean isForeground) {
        final TrackPageHolder holder = getViewHolder(trackPage);
        final boolean playSessionIsActive = playStateEvent.playSessionIsActive();
        holder.playControlsHolder.setVisibility(playSessionIsActive ? View.GONE : View.VISIBLE);
        holder.footerPlayToggle.setChecked(playSessionIsActive);
        setWaveformPlayState(holder, playStateEvent, isCurrentTrack);
        setViewPlayState(holder, playStateEvent, isCurrentTrack);

        holder.timestamp.setBufferingMode(isCurrentTrack && playStateEvent.isBuffering());

        if (playStateEvent.playSessionIsActive() && !isCurrentTrack) {
            for (ProgressAware view : getViewHolder(trackPage).progressAwareViews) {
                view.clearProgress();
            }
        }
        configureAdOverlay(playStateEvent, isCurrentTrack, isForeground, holder);
    }

    private void configureAdOverlay(PlayStateEvent playStateEvent,
                                    boolean isCurrentTrack,
                                    boolean isForeground,
                                    TrackPageHolder holder) {
        if (isCurrentTrack) {
            if (playStateEvent.isPlayerPlaying() && isForeground) {
                holder.adOverlayController.show(true);
            } else if (playStateEvent.isPaused() || playStateEvent.getTransition().wasError()) {
                clearAdOverlay(holder);
            }
        }
    }

    @Override
    public void onPlayableReposted(View trackPage, RepostsStatusEvent.RepostStatus repostStatus) {
        final TrackPageHolder holder = getViewHolder(trackPage);
        final boolean isReposted = repostStatus.isReposted();
        holder.menuController.setIsUserRepost(isReposted);
        showRepostToast(trackPage.getContext(), isReposted);
    }

    @Override
    public void onPlayableLiked(View trackPage, LikesStatusEvent.LikeStatus likeStatus) {
        final TrackPageHolder holder = getViewHolder(trackPage);
        holder.likeToggle.setChecked(likeStatus.isUserLike());
        if (likeStatus.likeCount().isPresent()) {
            likeButtonPresenter.setLikeCount(holder.likeToggle, likeStatus.likeCount().get(),
                                             R.drawable.ic_player_liked, R.drawable.ic_player_like);
        }
    }

    void onPositionSet(View trackPage, int position, int size) {
        final TrackPageHolder holder = getViewHolder(trackPage);
        if (holder.hasNextButton()) {
            holder.nextButton.setVisibility(position == size - 1 ? View.INVISIBLE : View.VISIBLE);
        }
        if (holder.hasPreviousButton()) {
            holder.previousButton.setVisibility(position == 0 ? View.INVISIBLE : View.VISIBLE);
        }
    }

    @Override
    public void onBackground(View trackPage) {
        final TrackPageHolder viewHolder = getViewHolder(trackPage);
        viewHolder.waveformController.onBackground();
        castConnectionHelper.removeMediaRouterButton(viewHolder.mediaRouteButton);
        viewHolder.castPlayerStripController.unsubscribeFromEvents();
    }

    @Override
    public void onForeground(View trackPage) {
        final TrackPageHolder viewHolder = getViewHolder(trackPage);
        viewHolder.waveformController.onForeground();
        castConnectionHelper.addMediaRouterButton(viewHolder.mediaRouteButton);
        viewHolder.castPlayerStripController.subscribeToEvents();
    }

    @Override
    public void onDestroyView(View trackPage) {
        final TrackPageHolder viewHolder = getViewHolder(trackPage);
        viewHolder.artworkController.cancelProgressAnimations();
        viewHolder.waveformController.cancelProgressAnimations();
    }

    private void showRepostToast(final Context context, final boolean isReposted) {
        Toast.makeText(context, isReposted
                                ? R.string.reposted_to_followers
                                : R.string.unposted_to_followers, Toast.LENGTH_SHORT).show();
    }

    private void updateLikeStatus(View likeToggle) {
        final Urn trackUrn = (Urn) likeToggle.getTag();

        if (trackUrn != null) {
            listener.onToggleLike(isLiked(likeToggle), trackUrn);
        }
    }

    private void setWaveformPlayState(TrackPageHolder holder, PlayStateEvent state, boolean isCurrentTrack) {
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

    private void setViewPlayState(TrackPageHolder holder, PlayStateEvent state, boolean isCurrentTrack) {
        updateErrorState(holder, state, isCurrentTrack);
        holder.artworkController.setPlayState(state, isCurrentTrack);

        for (PlayerOverlayController playerOverlayController : holder.playerOverlayControllers) {
            playerOverlayController.setPlayState(state);
        }

        setTextBackgrounds(holder, state.playSessionIsActive());
    }

    private void updateErrorState(TrackPageHolder holder, PlayStateEvent state, boolean isCurrentTrack) {
        final PlaybackStateTransition transition = state.getTransition();
        if (isCurrentTrack && transition.wasError()) {
            holder.errorViewController.showError(getErrorStateFromPlayerState(transition));
        } else {
            holder.errorViewController.hideNonBlockedErrors();
        }
    }

    private ErrorViewController.ErrorState getErrorStateFromPlayerState(PlaybackStateTransition state) {
        switch (state.getReason()) {
            case ERROR_NOT_FOUND:
            case ERROR_FORBIDDEN:
                return ErrorViewController.ErrorState.UNPLAYABLE;
            default:
                return ErrorViewController.ErrorState.FAILED;
        }
    }

    private void setAdStateOnPlayerOverlay(TrackPageHolder holder, boolean isShown) {
        for (PlayerOverlayController playerOverlayController : holder.playerOverlayControllers) {
            playerOverlayController.setAdOverlayShown(isShown);
        }
    }

    private void setTextBackgrounds(TrackPageHolder holder, boolean visible) {
        holder.title.showBackground(visible);
        holder.user.showBackground(visible);
        holder.timestamp.showBackground(visible);
        holder.trackContext.showBackground(visible);
    }

    void onPageChange(View trackView) {
        TrackPageHolder holder = getViewHolder(trackView);
        holder.waveformController.scrubStateChanged(ScrubController.SCRUB_STATE_NONE);
        holder.menuController.dismiss();
    }

    @Override
    public void setProgress(View trackPage, PlaybackProgress progress) {
        if (!progress.isEmpty()) {
            for (ProgressAware view : getViewHolder(trackPage).progressAwareViews) {
                view.setProgress(progress);
            }
        }
    }

    @Override
    public void setCollapsed(View trackView) {
        onPlayerSlide(trackView, 0);
        getViewHolder(trackView).waveformController.setCollapsed();
        getViewHolder(trackView).adOverlayController.setCollapsed();
        getViewHolder(trackView).topStrip.setCollapsed();
    }

    @Override
    public void setExpanded(View trackView, PlayQueueItem playQueueItem, boolean isSelected) {
        onPlayerSlide(trackView, 1);
        getViewHolder(trackView).waveformController.setExpanded();
        getViewHolder(trackView).adOverlayController.setExpanded();
        getViewHolder(trackView).castPlayerStripController.updateWithoutAnimation();
        if (isSelected && getViewHolder(trackView).upsellView.getVisibility() == View.VISIBLE) {
            upsellImpressionController.recordUpsellViewed(playQueueItem);
        }
    }

    @Override
    public void onPlayerSlide(View trackView, float slideOffset) {
        TrackPageHolder holder = getViewHolder(trackView);

        final Iterable<View> fullScreenViews = getFullScreenViews(holder);
        slideHelper.configureViewsFromSlide(slideOffset,
                                            holder.footer,
                                            fullScreenViews,
                                            holder.fullyHideOnCollapseViews,
                                            holder.castPlayerStripController,
                                            holder.playerOverlayControllers);
        holder.waveformController.onPlayerSlide(slideOffset);

        getViewHolder(trackView).closeIndicator.setVisibility(slideOffset > 0 ? View.VISIBLE : View.GONE);
        getViewHolder(trackView).footer.setVisibility(slideOffset < 1 ? View.VISIBLE : View.GONE);
    }

    private Iterable<View> getFullScreenViews(TrackPageHolder holder) {
        if (holder.adOverlayController.isVisibleInFullscreen()) {
            return holder.fullScreenAdViews;
        } else if (holder.errorViewController.isShowingError()) {
            return holder.fullScreenErrorViews;
        } else {
            return holder.fullScreenViews;
        }
    }

    public boolean accept(View view) {
        return view.getTag() instanceof TrackPageHolder;
    }

    private void setClickListener(View.OnClickListener listener, Iterable<View> views) {
        for (View v : views) {
            v.setOnClickListener(listener);
        }
    }

    private boolean isLiked(View toggleLike) {
        return ((Checkable) toggleLike).isChecked();
    }

    private void setupSkipListener(View trackView, final SkipListener skipListener) {
        TrackPageHolder holder = getViewHolder(trackView);

        final View.OnClickListener nextListener = v -> skipListener.onNext();
        final View.OnClickListener previousListener = v -> skipListener.onPrevious();
        if (holder.hasNextButton()) {
            holder.nextButton.setOnClickListener(nextListener);
        }
        if (holder.hasPreviousButton()) {
            holder.previousButton.setOnClickListener(previousListener);
        }
    }

    private ScrubController.OnScrubListener createScrubViewAnimations(final TrackPageHolder holder) {
        return new ScrubController.OnScrubListener() {
            @Override
            public void scrubStateChanged(int newScrubState) {
                for (View v : holder.hideOnScrubViews) {
                    ObjectAnimator animator = ObjectAnimator.ofFloat(v, "alpha", v.getAlpha(),
                                                                     newScrubState == ScrubController.SCRUB_STATE_SCRUBBING ?
                                                                     0 : 1);
                    animator.setDuration(SCRUB_TRANSITION_ALPHA_DURATION);
                    animator.start();
                }
            }

            @Override
            public void displayScrubPosition(float actualPosition, float boundedPosition) {
                // no-op
            }
        };
    }

    private TrackPageHolder getViewHolder(View trackView) {
        return (TrackPageHolder) trackView.getTag();
    }

    private void setupHolder(View trackView) {
        final TrackPageHolder holder = new TrackPageHolder();
        holder.title = (JaggedTextView) trackView.findViewById(R.id.track_page_title);
        holder.user = (JaggedTextView) trackView.findViewById(R.id.track_page_user);
        holder.trackContext = (JaggedTextView) trackView.findViewById(R.id.track_page_context);

        holder.artworkView = (PlayerTrackArtworkView) trackView.findViewById(R.id.track_page_artwork);
        holder.artworkOverlayDark = trackView.findViewById(R.id.artwork_overlay_dark);
        holder.timestamp = (TimestampView) trackView.findViewById(R.id.timestamp);
        holder.likeToggle = (ToggleButton) trackView.findViewById(R.id.track_page_like);
        holder.more = trackView.findViewById(R.id.track_page_more);
        holder.close = trackView.findViewById(R.id.player_expanded_top_bar);
        holder.bottomClose = trackView.findViewById(R.id.player_bottom_close);
        holder.nextButton = trackView.findViewById(R.id.player_next);
        holder.previousButton = trackView.findViewById(R.id.player_previous);
        holder.playButton = trackView.findViewById(R.id.player_play);
        holder.profileLink = trackView.findViewById(R.id.profile_link);

        holder.shareButton = trackView.findViewById(R.id.track_page_share);
        holder.playQueueButton = trackView.findViewById(R.id.play_queue_button);
        holder.topStrip = (PlayerStripView) trackView.findViewById(R.id.player_strip);
        holder.upsellView = (PlayerUpsellView) trackView.findViewById(R.id.upsell_container);
        holder.topGradient = trackView.findViewById(R.id.top_gradient);

        // set initial media route button state
        holder.mediaRouteButton = (MediaRouteButton) trackView.findViewById(R.id.media_route_button);

        holder.footer = trackView.findViewById(R.id.footer_controls);
        holder.footerPlayToggle = (ToggleButton) trackView.findViewById(R.id.footer_toggle);
        holder.footerTitle = (TextView) trackView.findViewById(R.id.footer_title);
        holder.footerUser = (TextView) trackView.findViewById(R.id.footer_user);
        holder.footerQueueButton = trackView.findViewById(R.id.footer_play_queue_button);

        holder.adOverlayController = adOverlayControllerFactory.create(trackView, createAdOverlayListener(holder));

        final WaveformView waveformView = (WaveformView) trackView.findViewById(R.id.track_page_waveform);
        holder.waveformController = waveformControllerFactory.create(waveformView);
        holder.playerOverlayControllers = new PlayerOverlayController[]{
                playerOverlayControllerFactory.create(holder.artworkOverlayDark),
                playerOverlayControllerFactory.create(holder.artworkView.findViewById(R.id.artwork_overlay_image))
        };

        holder.artworkController = artworkControllerFactory.create(holder.artworkView);
        holder.waveformController.addScrubListener(holder.artworkController);
        holder.waveformController.addScrubListener(holder.timestamp);
        holder.waveformController.addScrubListener(createScrubViewAnimations(holder));
        holder.menuController = trackMenuControllerFactory.create(holder.more);
        holder.castPlayerStripController = castPlayerStripControllerFactory.create(holder.topStrip, holder.upsellView);

        holder.playControlsHolder = trackView.findViewById(R.id.play_controls);
        holder.closeIndicator = trackView.findViewById(R.id.player_close_indicator);
        holder.interstitialHolder = trackView.findViewById(R.id.interstitial_holder);

        for (PlayerOverlayController playerOverlayController : holder.playerOverlayControllers) {
            holder.waveformController.addScrubListener(playerOverlayController);
        }
        holder.waveformController.addScrubListener(holder.menuController);

        holder.more.setOnClickListener(view -> {
            clearAdOverlay(holder);
            holder.menuController.show();
        });

        View.OnClickListener shareClickListener = view -> holder.menuController.handleShare(view.getContext());

        holder.shareButton.setOnClickListener(shareClickListener);
        holder.likeToggle.setOnClickListener(this::updateLikeStatus);

        holder.populateViewSets(playQueueConfiguration);
        trackView.setTag(holder);

        holder.errorViewController = errorControllerFactory.create(trackView);
        holder.emptyViewController = emptyControllerFactory.create(trackView);
    }

    private AdOverlayController.AdOverlayListener createAdOverlayListener(final TrackPageHolder holder) {
        return new AdOverlayController.AdOverlayListener() {
            @Override
            public void onAdOverlayShown(boolean fullscreen) {
                setAdStateOnPlayerOverlay(holder, true);
                setTextBackgrounds(holder, false);
                holder.waveformController.hide();

                if (fullscreen) {
                    AnimUtils.hideViews(holder.hideOnAdViews);
                    castConnectionHelper.removeMediaRouterButton(holder.mediaRouteButton);
                    holder.shareButton.setVisibility(View.GONE);
                }
            }

            @Override
            public void onAdOverlayHidden(boolean fullscreen) {
                setAdStateOnPlayerOverlay(holder, false);
                setTextBackgrounds(holder, true);
                holder.waveformController.show();

                if (fullscreen) {
                    AnimUtils.showViews(holder.hideOnAdViews);
                    castConnectionHelper.addMediaRouterButton(holder.mediaRouteButton);
                }
            }
        };
    }

    static class TrackPageHolder {
        // Expanded player
        JaggedTextView title;
        JaggedTextView user;
        JaggedTextView trackContext;
        TimestampView timestamp;
        PlayerTrackArtworkView artworkView;
        View artworkOverlayDark;
        ToggleButton likeToggle;
        MediaRouteButton mediaRouteButton;
        View more;
        View close;
        View bottomClose;
        @Nullable
        View nextButton;
        @Nullable
        View previousButton;
        @Nullable
        View playButton;
        View closeIndicator;
        PlayerStripView topStrip;
        PlayerUpsellView upsellView;
        View profileLink;
        View playControlsHolder;
        View interstitialHolder;
        View shareButton;
        View playQueueButton;
        View topGradient;

        WaveformViewController waveformController;
        TrackPageMenuController menuController;
        PlayerArtworkController artworkController;
        PlayerOverlayController[] playerOverlayControllers;
        AdOverlayController adOverlayController;
        ErrorViewController errorViewController;
        EmptyViewController emptyViewController;
        CastPlayerStripController castPlayerStripController;

        // Footer player
        View footer;
        ToggleButton footerPlayToggle;
        TextView footerTitle;
        TextView footerUser;
        View footerQueueButton;

        // View sets
        Iterable<View> fullScreenViews;
        Iterable<View> fullScreenAdViews;
        Iterable<View> fullScreenErrorViews;
        Iterable<View> hideOnScrubViews;
        Iterable<View> hideOnErrorViews;
        Iterable<View> hideOnEmptyViews;
        Iterable<View> hideOnAdViews;
        Iterable<View> onClickViews;
        Iterable<View> fullyHideOnCollapseViews;
        Iterable<ProgressAware> progressAwareViews;

        private static final Predicate<View> PRESENT_IN_CONFIG = v -> v != null;


        void populateViewSets(PlayQueueConfiguration playQueueConfiguration) {
            List<View> hideOnScrub = Arrays.asList(title,
                                                   user,
                                                   trackContext,
                                                   closeIndicator,
                                                   mediaRouteButton,
                                                   nextButton,
                                                   previousButton,
                                                   playButton,
                                                   bottomClose,
                                                   upsellView,
                                                   topGradient,
                                                   getPlayQueueButtonOrNull(playQueueConfiguration));
            List<View> hideOnError = Arrays.asList(playButton, timestamp);
            List<View> hideOnEmpty = Arrays.asList(playButton, timestamp, bottomClose);
            List<View> clickViews = Arrays.asList(artworkView,
                                                  closeIndicator,
                                                  bottomClose,
                                                  playButton,
                                                  footer,
                                                  footerPlayToggle,
                                                  profileLink,
                                                  upsellView.getUpsellButton(),
                                                  getPlayQueueButtonOrNull(playQueueConfiguration));
            List<View> trackViews = Arrays.asList(close,
                                                  more,
                                                  likeToggle,
                                                  title,
                                                  user,
                                                  timestamp,
                                                  getPlayQueueButtonOrNull(playQueueConfiguration));


            fullScreenViews = Arrays.asList(title, user, trackContext, close, timestamp, interstitialHolder,
                                            upsellView, topGradient);
            fullScreenAdViews = singletonList(interstitialHolder);
            fullScreenErrorViews = Arrays.asList(title, user, trackContext, close, interstitialHolder);
            fullyHideOnCollapseViews = Collections.singletonList(profileLink);

            hideOnScrubViews = Iterables.filter(hideOnScrub, PRESENT_IN_CONFIG);
            hideOnErrorViews = Iterables.filter(hideOnError, PRESENT_IN_CONFIG);
            hideOnEmptyViews = Iterables.filter(hideOnEmpty, PRESENT_IN_CONFIG);
            onClickViews = Iterables.filter(clickViews, PRESENT_IN_CONFIG);
            hideOnAdViews = Iterables.filter(trackViews, PRESENT_IN_CONFIG);
            progressAwareViews = Lists.newArrayList(waveformController,
                                                    artworkController,
                                                    timestamp,
                                                    menuController);

        }

        @Nullable
        private View getPlayQueueButtonOrNull(PlayQueueConfiguration playQueueConfiguration) {
            return playQueueConfiguration.isEnabled() ? playQueueButton : null;
        }

        private boolean hasNextButton() {
            return nextButton != null;
        }

        private boolean hasPreviousButton() {
            return previousButton != null;
        }
    }

}
