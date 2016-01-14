package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.playback.Player.StateTransition;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdOverlayController;
import com.soundcloud.android.ads.AdOverlayControllerFactory;
import com.soundcloud.android.ads.OverlayAdData;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.payments.PlayerUpsellImpressionController;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.ui.progress.ProgressAware;
import com.soundcloud.android.playback.ui.progress.ScrubController;
import com.soundcloud.android.playback.ui.view.PlayerTrackArtworkView;
import com.soundcloud.android.playback.ui.view.TimestampView;
import com.soundcloud.android.playback.ui.view.WaveformView;
import com.soundcloud.android.playback.ui.view.WaveformViewController;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.android.util.AnimUtils;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.JaggedTextView;
import com.soundcloud.android.waveform.WaveformOperations;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Predicate;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import org.jetbrains.annotations.Nullable;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
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
import java.util.List;

class TrackPagePresenter implements PlayerPagePresenter<PlayerTrackState>, View.OnClickListener {

    private static final int SCRUB_TRANSITION_ALPHA_DURATION = 100;

    private final WaveformOperations waveformOperations;
    private final FeatureOperations featureOperations;
    private final TrackPageListener listener;
    private final CondensedNumberFormatter numberFormatter;
    private final WaveformViewController.Factory waveformControllerFactory;
    private final PlayerArtworkController.Factory artworkControllerFactory;
    private final PlayerOverlayController.Factory playerOverlayControllerFactory;
    private final TrackPageMenuController.Factory trackMenuControllerFactory;
    private final AdOverlayControllerFactory adOverlayControllerFactory;
    private final ErrorViewController.Factory errorControllerFactory;
    private final CastConnectionHelper castConnectionHelper;
    private final Resources resources;
    private final PlayerUpsellImpressionController upsellImpressionController;

    private final SlideAnimationHelper helper = new SlideAnimationHelper();

    @Inject
    public TrackPagePresenter(WaveformOperations waveformOperations,
                              FeatureOperations featureOperations,
                              TrackPageListener listener,
                              CondensedNumberFormatter numberFormatter,
                              WaveformViewController.Factory waveformControllerFactory,
                              PlayerArtworkController.Factory artworkControllerFactory,
                              PlayerOverlayController.Factory playerOverlayControllerFactory,
                              TrackPageMenuController.Factory trackMenuControllerFactory,
                              AdOverlayControllerFactory adOverlayControllerFactory,
                              ErrorViewController.Factory errorControllerFactory,
                              CastConnectionHelper castConnectionHelper,
                              Resources resources,
                              PlayerUpsellImpressionController upsellImpressionController) {
        this.waveformOperations = waveformOperations;
        this.featureOperations = featureOperations;
        this.listener = listener;
        this.numberFormatter = numberFormatter;
        this.waveformControllerFactory = waveformControllerFactory;
        this.artworkControllerFactory = artworkControllerFactory;
        this.playerOverlayControllerFactory = playerOverlayControllerFactory;
        this.trackMenuControllerFactory = trackMenuControllerFactory;
        this.adOverlayControllerFactory = adOverlayControllerFactory;
        this.errorControllerFactory = errorControllerFactory;
        this.castConnectionHelper = castConnectionHelper;
        this.resources = resources;
        this.upsellImpressionController = upsellImpressionController;
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
            case R.id.profile_link:
                final Context activityContext = view.getContext();
                final Urn userUrn = (Urn) view.getTag();
                listener.onGotoUser(activityContext, userUrn);
                break;
            case R.id.upsell_button:
                listener.onUpsell(view.getContext(), (Urn) view.getTag());
                break;
            default:
                throw new IllegalArgumentException("Unexpected view ID: "
                        + resources.getResourceName(view.getId()));
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
    public void bindItemView(View trackView, PlayerTrackState trackState) {
        final TrackPageHolder holder = getViewHolder(trackView);
        holder.title.setText(trackState.getTitle());
        holder.user.setText(trackState.getUserName());
        holder.profileLink.setTag(trackState.getUserUrn());
        setCastDeviceName(trackView, castConnectionHelper.getDeviceName());
        bindStationsContext(trackState, holder);

        holder.artworkController.loadArtwork(trackState.getUrn(), trackState.isCurrentTrack(),
                trackState.getViewVisibilityProvider());

        holder.timestamp.setInitialProgress(trackState.getPlayableDuration(), trackState.getFullDuration());
        holder.menuController.setTrack(trackState);
        holder.waveformController.setWaveform(waveformOperations.waveformDataFor(trackState.getUrn(),
                trackState.getWaveformUrl()), trackState.isForeground());

        holder.artworkController.setFullDuration(trackState.getFullDuration());
        holder.waveformController.setDurations(trackState.getPlayableDuration(), trackState.getFullDuration());

        setLikeCount(holder, trackState.getLikeCount());
        holder.likeToggle.setChecked(trackState.isUserLike());
        holder.likeToggle.setTag(trackState.getUrn());
        holder.shareButton.setTag(trackState.getUrn());

        holder.footerUser.setText(trackState.getUserName());
        holder.footerTitle.setText(trackState.getTitle());

        final boolean blocked = trackState.isBlocked();
        holder.artworkView.setEnabled(!blocked);
        updatePlayButton(holder, blocked);

        holder.upsellButton.setTag(trackState.getUrn());

        if (featureOperations.upsellHighTier()) {
            holder.previewIndicator.setVisibility(trackState.isSnipped() ? View.VISIBLE : View.GONE);
            holder.upsellButton.setVisibility(trackState.shouldUpsell() ? View.VISIBLE : View.GONE);
        } else {
            holder.previewIndicator.setVisibility(View.GONE);
            holder.upsellButton.setVisibility(View.GONE);
        }

        if (blocked) {
            holder.errorViewController.showError(ErrorViewController.ErrorState.BLOCKED);
        } else {
            if (!holder.errorViewController.isShowingError()) {
                holder.timestamp.setVisibility(View.VISIBLE);
            }
        }

        setClickListener(this, holder.onClickViews);
    }

    private void updatePlayButton(TrackPageHolder holder, boolean blocked) {
        if (holder.playButton != null) {
            holder.playButton.setEnabled(!blocked);
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
    public void setCastDeviceName(View view, String deviceName) {
        getViewHolder(view).castDeviceName.setText(deviceName);
    }

    @Override
    public void onViewSelected(View view, PlayQueueItem playQueueItem, boolean isExpanded) {
        if (isExpanded && getViewHolder(view).upsellButton.getVisibility() == View.VISIBLE) {
            upsellImpressionController.recordUpsellViewed(playQueueItem);
        }
    }

    public void setAdOverlay(View view, OverlayAdData adData) {
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
        holder.user.setText(ScTextUtils.EMPTY_STRING);
        holder.title.setText(ScTextUtils.EMPTY_STRING);

        holder.trackContext.setVisibility(View.GONE);

        holder.likeToggle.setChecked(false);
        holder.likeToggle.setEnabled(true);

        holder.artworkController.reset();
        holder.waveformController.reset();

        holder.footerUser.setText(ScTextUtils.EMPTY_STRING);
        holder.footerTitle.setText(ScTextUtils.EMPTY_STRING);

        holder.timestamp.setVisibility(View.GONE);
        holder.errorViewController.hideError();

        holder.previewIndicator.setVisibility(View.GONE);
        holder.upsellButton.setVisibility(View.GONE);

        return view;
    }

    @Override
    public void setPlayState(View trackPage, StateTransition stateTransition, boolean isCurrentTrack, boolean isForeground) {
        final TrackPageHolder holder = getViewHolder(trackPage);
        final boolean playSessionIsActive = stateTransition.playSessionIsActive();
        holder.playControlsHolder.setVisibility(playSessionIsActive ? View.GONE : View.VISIBLE);
        holder.footerPlayToggle.setChecked(playSessionIsActive);
        setWaveformPlayState(holder, stateTransition, isCurrentTrack);
        setViewPlayState(holder, stateTransition, isCurrentTrack);

        holder.timestamp.setBufferingMode(isCurrentTrack && stateTransition.isBuffering());

        if (stateTransition.playSessionIsActive() && !isCurrentTrack) {
            for (ProgressAware view : getViewHolder(trackPage).progressAwareViews) {
                view.clearProgress();
            }
        }
        configureAdOverlay(stateTransition, isCurrentTrack, isForeground, holder);
    }

    private void configureAdOverlay(StateTransition stateTransition, boolean isCurrentTrack, boolean isForeground, TrackPageHolder holder) {
        if (isCurrentTrack) {
            if (stateTransition.isPlayerPlaying() && isForeground) {
                holder.adOverlayController.show(true);
            } else if (stateTransition.isPaused() || stateTransition.wasError()) {
                clearAdOverlay(holder);
            }
        }
    }

    @Override
    public void onPlayableUpdated(View trackPage, EntityStateChangedEvent trackChangedEvent) {
        final TrackPageHolder holder = getViewHolder(trackPage);
        final PropertySet changeSet = trackChangedEvent.getNextChangeSet();

        if (changeSet.contains(PlayableProperty.IS_USER_LIKE)) {
            holder.likeToggle.setChecked(changeSet.get(PlayableProperty.IS_USER_LIKE));
        }
        if (changeSet.contains(PlayableProperty.LIKES_COUNT)) {
            setLikeCount(holder, changeSet.get(PlayableProperty.LIKES_COUNT));
        }

        if (changeSet.contains(PlayableProperty.IS_USER_REPOST)) {
            final boolean isReposted = changeSet.get(PlayableProperty.IS_USER_REPOST);
            holder.menuController.setIsUserRepost(isReposted);

            if (trackChangedEvent.getKind() == EntityStateChangedEvent.REPOST) {
                showRepostToast(trackPage.getContext(), isReposted);
            }
        }
    }

    public void onPositionSet(View trackPage, int position, int size) {
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
    }

    @Override
    public void onForeground(View trackPage) {
        final TrackPageHolder viewHolder = getViewHolder(trackPage);
        viewHolder.waveformController.onForeground();
        castConnectionHelper.addMediaRouterButton(viewHolder.mediaRouteButton);
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

    private void setLikeCount(TrackPageHolder holder, int count) {
        holder.likeToggle.setText(count > 0 ? numberFormatter.format(count) : Strings.EMPTY);
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
        updateErrorState(holder, state, isCurrentTrack);

        if (state.playSessionIsActive() && isCurrentTrack) {
            if (state.isPlayerPlaying()) {
                holder.artworkController.showPlayingState(state.getProgress());
            } else {
                holder.artworkController.showIdleState(state.getProgress());
            }
        } else {
            holder.artworkController.showIdleState();
        }

        for (PlayerOverlayController playerOverlayController : holder.playerOverlayControllers) {
            playerOverlayController.setPlayState(state);
        }

        setTextBackgrounds(holder, state.playSessionIsActive());
    }

    private void updateErrorState(TrackPageHolder holder, StateTransition state, boolean isCurrentTrack) {
        if (isCurrentTrack && state.wasError()) {
            holder.errorViewController.showError(getErrorStateFromPlayerState(state));
        } else {
            holder.errorViewController.hideNonBlockedErrors();
        }
    }

    private ErrorViewController.ErrorState getErrorStateFromPlayerState(StateTransition state) {
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

    public void onPageChange(View trackView) {
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
    }

    @Override
    public void setExpanded(View trackView, PlayQueueItem playQueueItem, boolean isSelected) {
        onPlayerSlide(trackView, 1);
        getViewHolder(trackView).waveformController.setExpanded();
        getViewHolder(trackView).adOverlayController.setExpanded();
        if (isSelected && getViewHolder(trackView).upsellButton.getVisibility() == View.VISIBLE) {
            upsellImpressionController.recordUpsellViewed(playQueueItem);
        }
    }

    @Override
    public void onPlayerSlide(View trackView, float slideOffset) {
        TrackPageHolder holder = getViewHolder(trackView);

        final Iterable<View> fullScreenViews = getFullScreenViews(holder);
        helper.configureViewsFromSlide(slideOffset, holder.footer, fullScreenViews, holder.playerOverlayControllers);
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
                    ObjectAnimator animator = ObjectAnimator.ofFloat(v, "alpha", v.getAlpha(),
                            newScrubState == ScrubController.SCRUB_STATE_SCRUBBING ? 0 : 1);
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

        holder.castDeviceName = (TextView) trackView.findViewById(R.id.cast_device_name);
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
        holder.shareButton = trackView.findViewById(R.id.track_page_share);
        holder.upsellButton = trackView.findViewById(R.id.upsell_button);
        holder.previewIndicator = trackView.findViewById(R.id.preview_indicator);

        // set initial media route button state
        holder.mediaRouteButton = (MediaRouteButton) trackView.findViewById(R.id.media_route_button);

        holder.footer = trackView.findViewById(R.id.footer_controls);
        holder.footerPlayToggle = (ToggleButton) trackView.findViewById(R.id.footer_toggle);
        holder.footerTitle = (TextView) trackView.findViewById(R.id.footer_title);
        holder.footerUser = (TextView) trackView.findViewById(R.id.footer_user);

        holder.adOverlayController = adOverlayControllerFactory.create(trackView, createAdOverlayListener(holder));

        final WaveformView waveformView = (WaveformView) trackView.findViewById(R.id.track_page_waveform);
        holder.waveformController = waveformControllerFactory.create(waveformView);
        holder.playerOverlayControllers = new PlayerOverlayController[]{
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
        holder.interstitialHolder = trackView.findViewById(R.id.interstitial_holder);

        for (PlayerOverlayController playerOverlayController : holder.playerOverlayControllers) {
            holder.waveformController.addScrubListener(playerOverlayController);
        }
        holder.waveformController.addScrubListener(holder.menuController);

        holder.more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearAdOverlay(holder);
                holder.menuController.show();
            }
        });

        holder.shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.menuController.handleShare(view.getContext());
            }
        });

        holder.likeToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateLikeStatus(view);
            }
        });

        holder.populateViewSets();
        trackView.setTag(holder);

        holder.errorViewController = errorControllerFactory.create(trackView);
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
        TextView castDeviceName;
        TimestampView timestamp;
        PlayerTrackArtworkView artworkView;
        ToggleButton likeToggle;
        MediaRouteButton mediaRouteButton;
        View more;
        View close;
        View bottomClose;
        @Nullable View nextButton;
        @Nullable View previousButton;
        @Nullable View playButton;
        View closeIndicator;
        View previewIndicator;
        View upsellButton;
        View profileLink;
        View playControlsHolder;
        View interstitialHolder;
        View shareButton;

        WaveformViewController waveformController;
        TrackPageMenuController menuController;
        PlayerArtworkController artworkController;
        PlayerOverlayController[] playerOverlayControllers;
        AdOverlayController adOverlayController;
        ErrorViewController errorViewController;

        // Footer player
        View footer;
        ToggleButton footerPlayToggle;
        TextView footerTitle;
        TextView footerUser;

        // View sets
        Iterable<View> fullScreenViews;
        Iterable<View> fullScreenAdViews;
        Iterable<View> fullScreenErrorViews;
        Iterable<View> hideOnScrubViews;
        Iterable<View> hideOnErrorViews;
        Iterable<View> hideOnAdViews;
        Iterable<View> onClickViews;
        Iterable<ProgressAware> progressAwareViews;

        private static final Predicate<View> PRESENT_IN_CONFIG = new Predicate<View>() {
            @Override
            public boolean apply(@Nullable View v) {
                return v != null;
            }
        };

        public void populateViewSets() {
            List<View> hideOnScrub = Arrays.asList(title, user, trackContext, closeIndicator, nextButton, previousButton, playButton, bottomClose);
            List<View> hideOnError = Arrays.asList(playButton, more, likeToggle, timestamp, shareButton);
            List<View> clickViews = Arrays.asList(artworkView, close, bottomClose, playButton, footer, footerPlayToggle, profileLink, previewIndicator, upsellButton);

            fullScreenViews = Arrays.asList(title, user, trackContext, close, timestamp, interstitialHolder);
            fullScreenAdViews = Arrays.asList(interstitialHolder);
            fullScreenErrorViews = Arrays.asList(title, user, trackContext, close, interstitialHolder);

            hideOnScrubViews = Iterables.filter(hideOnScrub, PRESENT_IN_CONFIG);
            hideOnErrorViews = Iterables.filter(hideOnError, PRESENT_IN_CONFIG);
            onClickViews = Iterables.filter(clickViews, PRESENT_IN_CONFIG);
            hideOnAdViews = Arrays.asList(close, more, likeToggle, shareButton, title, user, timestamp, castDeviceName);
            progressAwareViews = Lists.<ProgressAware>newArrayList(waveformController, artworkController, timestamp, menuController);
        }

        private boolean hasNextButton() {
            return nextButton != null;
        }

        private boolean hasPreviousButton() {
            return previousButton != null;
        }
    }

}
