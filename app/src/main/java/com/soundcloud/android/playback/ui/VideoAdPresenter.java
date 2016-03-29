package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.Player;
import com.soundcloud.android.playback.mediaplayer.MediaPlayerAdapter;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.java.collections.Iterables;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class VideoAdPresenter extends AdPagePresenter<VideoPlayerAd> implements View.OnClickListener {

    private enum UIState {
        INITIAL, ACTIVE, INACTIVE, PAUSED
    }

    private static final long FADE_IN_DURATION_MS = 500L;
    private static final long FADE_OUT_DURATION_MS = 1000L;
    private static final long FADE_OUT_OFFSET_MS = 2000L;

    private final MediaPlayerAdapter mediaPlayerAdapter;
    private final ImageOperations imageOperations;
    private final AdPageListener listener;
    private final PlayerOverlayController.Factory playerOverlayControllerFactory;
    private final DeviceHelper deviceHelper;
    private final Resources resources;

    @Inject
    public VideoAdPresenter(MediaPlayerAdapter mediaPlayerAdapter, ImageOperations imageOperations,
                            AdPageListener listener, PlayerOverlayController.Factory playerOverlayControllerFactory,
                            DeviceHelper deviceHelper, Resources resources) {
        this.mediaPlayerAdapter = mediaPlayerAdapter;
        this.imageOperations = imageOperations;
        this.listener = listener;
        this.playerOverlayControllerFactory = playerOverlayControllerFactory;
        this.deviceHelper = deviceHelper;
        this.resources = resources;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.video_overlay:
                setActiveUI(getViewHolder(view), view.getContext());
                break;
            case R.id.player_play:
            case R.id.video_pause_control:
            case R.id.video_view:
                listener.onTogglePlay();
                break;
            case R.id.player_next:
                listener.onNext();
                break;
            case R.id.player_previous:
                listener.onPrevious();
                break;
            case R.id.video_fullscreen_control:
                listener.onFullscreen();
                break;
            case R.id.video_shrink_control:
                listener.onShrink();
                break;
            case R.id.cta_button:
                listener.onClickThrough();
                break;
            case R.id.why_ads:
                listener.onAboutAds(view.getContext());
                break;
            case R.id.skip_ad:
                listener.onSkipAd();
                break;
            default:
                throw new IllegalArgumentException("Unexpected View ID");
        }
    }

    @Override
    public View createItemView(ViewGroup container, SkipListener skipListener) {
        final View adView = LayoutInflater.from(container.getContext()).inflate(R.layout.player_ad_video_page, container, false);
        final Holder holder = new Holder(adView, playerOverlayControllerFactory);
        adView.setTag(holder);
        setVideoViewHolder(holder);
        holder.videoOverlay.setTag(holder);
        resetSkipButton(holder, resources);
        return adView;
    }

    @Override
    public View clearItemView(View view) {
        final Holder holder = getViewHolder(view);
        resetUI(holder);
        return view;
    }

    @Override
    public void bindItemView(View adView, VideoPlayerAd playerAd) {
        final Holder holder = getViewHolder(adView);
        setupLoadingStateViews(holder, playerAd.isLetterboxVideo(), false);
        adjustLayoutForVideo(adView, playerAd, holder);
        resetSkipButton(holder, resources);
        displayPreview(playerAd, holder, imageOperations, resources);
        styleCallToActionButton(holder, playerAd, resources);
        setClickListener(this, holder.onClickViews);
    }

    private void adjustLayoutForVideo(View adView, VideoPlayerAd playerAd, Holder holder) {
        final LayoutParams layoutParams = adjustedVideoViewLayoutParams(playerAd, holder);
        final int backgroundColor = resources.getColor(isOrientationLandscape() ? R.color.ad_landscape_video_background : R.color.ad_default_background);

        adView.setBackgroundColor(backgroundColor);
        holder.videoSurfaceView.setLayoutParams(layoutParams);
        holder.videoOverlayContainer.setLayoutParams(layoutParams);
        holder.letterboxBackground.setLayoutParams(layoutParams);
        holder.fullscreenButton.setVisibility(playerAd.isLetterboxVideo() && isOrientationPortrait() ? View.VISIBLE : View.GONE);
        holder.shrinkButton.setVisibility(playerAd.isLetterboxVideo() && isOrientationLandscape() ? View.VISIBLE : View.GONE);
        holder.setupFadingInterface(playerAd.isVerticalVideo() || !isOrientationPortrait());

        if (holder.getUIState() != UIState.INITIAL) {
            setupLoadingStateViews(holder, playerAd.isLetterboxVideo(), true);
            setInactiveUI(holder, adView.getContext(), true);
        }
    }

    private LayoutParams adjustedVideoViewLayoutParams(VideoPlayerAd playerAd, Holder holder) {
        final LayoutParams layoutParams = holder.videoSurfaceView.getLayoutParams();

        if (playerAd.isVerticalVideo()) { // Vertical video view should scale like ImageView's CENTER_CROP
            final int sourceWidth = playerAd.getFirstSource().getWidth();
            final int sourceHeight = playerAd.getFirstSource().getHeight();
            final float scaleFactor = centerCropScaleFactor(holder.videoContainer, sourceWidth, sourceHeight);
            layoutParams.width = (int) ((float) sourceWidth * scaleFactor);
            layoutParams.height = (int) ((float) sourceHeight * scaleFactor);
        } else if (isOrientationPortrait()) {
            final int horizontalPadding = 2 * ViewUtils.dpToPx(resources, 5);
            layoutParams.width = holder.videoContainer.getWidth() - horizontalPadding;
            layoutParams.height = (int) ((float) layoutParams.width / playerAd.getVideoProportion());
        } else { // Landscape View
            layoutParams.height = holder.videoContainer.getHeight();
            layoutParams.width = (int) ((float) layoutParams.height * playerAd.getVideoProportion());
        }

        return layoutParams;
    }

    // Scale the video while maintaining aspect ratio so that both dimensions (width and height)
    // of the view will be equal to or larger than the corresponding dimension of the player.
    private float centerCropScaleFactor(View containerView, float sourceWidth, float sourceHeight) {
        final int viewWidth = containerView.getWidth();
        final int viewHeight = containerView.getHeight();
        float scaleFactor;
        if (sourceWidth * viewHeight > viewWidth * sourceHeight) {
            scaleFactor = viewHeight / sourceHeight;
        } else {
            scaleFactor = viewWidth / sourceWidth;
        }
        return scaleFactor;
    }

    private boolean isOrientationPortrait() {
        return deviceHelper.isOrientation(Configuration.ORIENTATION_PORTRAIT);
    }

    private boolean isOrientationLandscape() {
        return deviceHelper.isOrientation(Configuration.ORIENTATION_LANDSCAPE);
    }

    public void setVideoViewHolder(Holder holder) {
        final SurfaceHolder surfaceHolder = holder.videoSurfaceView.getHolder();
        surfaceHolder.addCallback(mediaPlayerAdapter);
    }

    @Override
    public void setProgress(View adView, PlaybackProgress progress) {
        updateSkipStatus(getViewHolder(adView), progress, resources);
    }

    @Override
    public void setPlayState(View adPage, Player.StateTransition stateTransition, boolean isCurrentItem, boolean isForeground) {
        final Holder holder = getViewHolder(adPage);
        holder.playControlsHolder.setVisibility(stateTransition.playSessionIsActive() ? View.GONE : View.VISIBLE);
        holder.playerOverlayController.setPlayState(stateTransition);
        setLoadingState(holder, stateTransition, isCurrentItem);

        if (isCurrentItem) {
            if (holder.getUIState() == UIState.INITIAL && stateTransition.isPlayerPlaying()) {
                setInactiveUI(holder, adPage.getContext(), true);
            } else if (holder.getUIState() != UIState.INITIAL && !stateTransition.playSessionIsActive()) {
                setPausedUI(holder);
            } else if (holder.getUIState() == UIState.PAUSED && stateTransition.playSessionIsActive()) {
                setInactiveUI(holder, adPage.getContext(), false);
            }
        }
    }

    private void setLoadingState(Holder holder, Player.StateTransition stateTransition, boolean isCurrentItem) {
        if (isCurrentItem) {
            holder.videoProgress.setVisibility(stateTransition.isBuffering() && stateTransition.playSessionIsActive() ? View.VISIBLE : View.GONE);
            if (stateTransition.isPlayerPlaying() && !isVideoSurfaceVisible(holder)) {
                holder.videoSurfaceView.setVisibility(View.VISIBLE);
            }
        } else {
            holder.videoProgress.setVisibility(stateTransition.playSessionIsActive() ? View.VISIBLE : View.GONE);
        }
    }

    private boolean isVideoSurfaceVisible(Holder holder) {
       return holder.videoSurfaceView.getVisibility() == View.VISIBLE;
    }

    private void setupLoadingStateViews(Holder holder, boolean isLetterboxVideo, boolean videoAlreadyStarted) {
        final boolean playControlsVisible = holder.playControlsHolder.getVisibility() == View.VISIBLE;
        holder.videoProgress.setVisibility(playControlsVisible || videoAlreadyStarted ? View.GONE : View.VISIBLE);
        holder.videoSurfaceView.setVisibility(videoAlreadyStarted ? View.VISIBLE : View.GONE);
        if (isLetterboxVideo) {
            holder.letterboxBackground.setVisibility(videoAlreadyStarted ? View.GONE : View.VISIBLE);
            holder.videoLayoutControls.setVisibility(videoAlreadyStarted ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void setCollapsed(View adPage) {
        // no-op (video player locked)
    }

    @Override
    public void setExpanded(View trackPage, PlayQueueItem playQueueItem, boolean isSelected) {
        // no-op (video player locked)
    }

    @Override
    public void onPlayerSlide(View adPage, float position) {
        // no-op (video player locked)
    }

    @Override
    public void onViewSelected(View view, PlayQueueItem value, boolean isExpanded) {
        // no-op
    }

    @Override
    public void onForeground(View adPage) {
        setVideoViewHolder(getViewHolder(adPage));
    }

    @Override
    public void onBackground(View adPage) {
        final SurfaceHolder surfaceHolder = getViewHolder(adPage).videoSurfaceView.getHolder();
        surfaceHolder.removeCallback(mediaPlayerAdapter);
    }

    private Holder getViewHolder(View videoPage) {
        return (Holder) videoPage.getTag();
    }

    private AnimationSet fadeInOutAnimation(Holder holder, Context context) {
        final AnimationSet animation = new AnimationSet(false);
        animation.addAnimation(fadeInAnimation(context));
        animation.addAnimation(fadeOutAnimation(context));
        animation.setAnimationListener(new UIAnimationListener(holder));
        return animation;
    }

    private Animation fadeInAnimation(Context context) {
        final Animation animation = AnimationUtils.loadAnimation(context, R.anim.abc_fade_in);
        animation.setDuration(FADE_IN_DURATION_MS);
        animation.setInterpolator(new DecelerateInterpolator(2.0f));
        return animation;
    }

    private Animation fadeOutAnimation(Context context) {
        final Animation animation = AnimationUtils.loadAnimation(context, R.anim.abc_fade_out);
        animation.setStartOffset(FADE_OUT_OFFSET_MS);
        animation.setDuration(FADE_OUT_DURATION_MS);
        animation.setInterpolator(new AccelerateInterpolator(2.0f));
        return animation;
    }

    private class UIAnimationListener implements Animation.AnimationListener {

        private final Holder holder;

        UIAnimationListener(Holder holder) {
           this.holder = holder;
        }

        @Override
        public void onAnimationStart(Animation animation) {
            // no-op
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            // If pause view was clicked, then don't set state to Inactive (Animation should be cleared & state set to Paused)
            if (holder.currentUIState == UIState.ACTIVE) {
                holder.setUIState(UIState.INACTIVE);
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
            // no-op
        }
    }

    private void resetUI(Holder holder) {
        setInitialUI(holder);
    }

    private void setActiveUI(Holder holder, Context context) {
        if (holder.currentUIState == UIState.INACTIVE) {
            setAnimation(holder.fadingViewsWithPause, fadeInOutAnimation(holder, context));
            holder.setUIState(UIState.ACTIVE);
        }
    }

    private void setInactiveUI(Holder holder, Context context, boolean withFade) {
        if (withFade) {
            setAnimation(holder.fadingViews, fadeOutAnimation(context));
        } else {
            setVisibility(false, holder.fadingViews);
        }
        holder.setUIState(UIState.INACTIVE);
    }

    private void setInitialUI(Holder holder) {
        setVisibility(true, holder.fadingViews);
        holder.setUIState(UIState.INITIAL);
    }

    private void setPausedUI(Holder holder) {
        clearAnimation(holder.fadingViewsWithPause);
        setVisibility(true, holder.fadingViews);
        holder.setUIState(UIState.PAUSED);
    }

    static class Holder extends AdHolder {

        private final View videoContainer;
        private final SurfaceView videoSurfaceView;
        private final View videoOverlayContainer;
        private final View videoLayoutControls;
        private final View videoOverlay;

        private final View pauseButton;
        private final View fullscreenButton;
        private final View shrinkButton;
        private final View advertisementLabel;

        private final View videoProgress;
        private final View letterboxBackground;

        private final PlayerOverlayController playerOverlayController;

        private final Iterable<View> onClickViews;
        Iterable<View> fadingViews = Collections.emptyList();
        Iterable<View> fadingViewsWithPause = Collections.emptyList();

        private UIState currentUIState = UIState.INITIAL;

        Holder(View adView, PlayerOverlayController.Factory playerOverlayControllerFactory) {
            super(adView);
            videoContainer = adView.findViewById(R.id.video_container);
            videoSurfaceView = (SurfaceView) adView.findViewById(R.id.video_view);
            videoOverlayContainer = adView.findViewById(R.id.video_overlay_container);
            videoLayoutControls = adView.findViewById(R.id.video_layout_control);
            videoOverlay = adView.findViewById(R.id.video_overlay);

            pauseButton = adView.findViewById(R.id.video_pause_control);
            fullscreenButton = adView.findViewById(R.id.video_fullscreen_control);
            shrinkButton = adView.findViewById(R.id.video_shrink_control);
            advertisementLabel = adView.findViewById(R.id.advertisement);

            videoProgress = adView.findViewById(R.id.video_progress);
            letterboxBackground = adView.findViewById(R.id.letterbox_background);

            playerOverlayController = playerOverlayControllerFactory.create(videoOverlay);

            List<View> clickViews = Arrays.asList(playButton, nextButton, previousButton, shrinkButton,
                    fullscreenButton, pauseButton, videoOverlay, videoSurfaceView, ctaButton, whyAds, skipAd);

            onClickViews = Iterables.filter(clickViews, presentInConfig);
        }

        UIState getUIState() {
            return currentUIState;
        }

        void setUIState(UIState state) {
            currentUIState = state;
        }

        void setupFadingInterface(boolean enableAllFadeableElements) {
            final List<View> fadeViews = enableAllFadeableElements ?
                    getAllFadeableElementViews() : Collections.singletonList(videoLayoutControls);
            final List<View> fadeViewsWithPause = new ArrayList<>(fadeViews);
            fadeViewsWithPause.add(pauseButton);
            fadingViews = Iterables.filter(fadeViews, presentInConfig);
            fadingViewsWithPause = Iterables.filter(fadeViewsWithPause, presentInConfig);
        }

        private List<View> getAllFadeableElementViews() {
            return Arrays.asList(advertisementLabel, whyAds, ctaButton, previewContainer, videoLayoutControls);
        }

    }
}
