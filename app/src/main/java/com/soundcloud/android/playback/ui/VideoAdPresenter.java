package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.PlayStateEvent;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.java.collections.Iterables;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class VideoAdPresenter extends AdPagePresenter<VideoPlayerAd> implements View.OnClickListener {

    private enum UIState {
        INITIAL, INACTIVE, PAUSED
    }

    private static final long FADE_OUT_DURATION_MS = 1000L;
    private static final long FADE_OUT_OFFSET_MS = 2000L;

    private final ImageOperations imageOperations;
    private final AdPageListener listener;
    private final PlayerOverlayController.Factory playerOverlayControllerFactory;
    private final DeviceHelper deviceHelper;
    private final Resources resources;

    @Inject
    public VideoAdPresenter(ImageOperations imageOperations,
                            AdPageListener listener, PlayerOverlayController.Factory playerOverlayControllerFactory,
                            DeviceHelper deviceHelper, Resources resources) {
        this.imageOperations = imageOperations;
        this.listener = listener;
        this.playerOverlayControllerFactory = playerOverlayControllerFactory;
        this.deviceHelper = deviceHelper;
        this.resources = resources;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.player_play:
            case R.id.video_view:
            case R.id.video_overlay:
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
                listener.onClickThrough(view.getContext());
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
        final View adView = LayoutInflater.from(container.getContext())
                                          .inflate(R.layout.player_ad_video_page, container, false);
        final Holder holder = new Holder(adView, playerOverlayControllerFactory);
        adView.setTag(holder);
        holder.videoOverlay.setTag(holder);
        return adView;
    }

    @Override
    public View clearItemView(View view) {
        final Holder holder = getViewHolder(view);
        setInitialUI(holder);
        return view;
    }

    @Override
    public void bindItemView(View adView, VideoPlayerAd playerAd) {
        final Holder holder = getViewHolder(adView);
        setupLoadingStateViews(holder, playerAd.isLetterboxVideo(), false);
        adjustLayoutForVideo(adView, playerAd, holder);
        displayPreview(playerAd, holder, imageOperations, resources);
        styleCallToActionButton(holder, playerAd, resources);
        setClickListener(this, holder.onClickViews);
        setupSkipButton(holder, playerAd);
    }

    TextureView getVideoTexture(View adView) {
        return getViewHolder(adView).videoTextureView;
    }

    private void adjustLayoutForVideo(View adView, VideoPlayerAd playerAd, Holder holder) {
        final LayoutParams layoutParams = adjustedVideoViewLayoutParams(playerAd, holder);
        final int backgroundColor = resources.getColor(isOrientationLandscape() ?
                                                       R.color.ad_landscape_video_background :
                                                       R.color.ad_default_background);

        adView.setBackgroundColor(backgroundColor);
        holder.videoTextureView.setLayoutParams(layoutParams);
        holder.letterboxBackground.setLayoutParams(layoutParams);
        if (isOrientationPortrait()) {
            holder.videoOverlayContainer.setLayoutParams(layoutParams);
        }
        holder.fullscreenButton.setVisibility(playerAd.isLetterboxVideo() && isOrientationPortrait() ?
                                              View.VISIBLE :
                                              View.GONE);
        holder.shrinkButton.setVisibility(playerAd.isLetterboxVideo() && isOrientationLandscape() ?
                                          View.VISIBLE :
                                          View.GONE);
        holder.setupFadingInterface(playerAd.isVerticalVideo() || !isOrientationPortrait());

        if (!holder.isUIState(UIState.INITIAL)) {
            setupLoadingStateViews(holder, playerAd.isLetterboxVideo(), true);
            setInactiveUI(holder, adView.getContext());
        }
    }

    private LayoutParams adjustedVideoViewLayoutParams(VideoPlayerAd playerAd, Holder holder) {
        final LayoutParams layoutParams = holder.videoTextureView.getLayoutParams();

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

    @Override
    public void setProgress(View adView, PlaybackProgress progress) {
        updateSkipStatus(getViewHolder(adView), progress, resources);
    }

    @Override
    public void setPlayState(View adPage,
                             PlayStateEvent playStateEvent,
                             boolean isCurrentItem,
                             boolean isForeground) {
        final Holder holder = getViewHolder(adPage);
        holder.playControlsHolder.setVisibility(playStateEvent.playSessionIsActive() ? View.GONE : View.VISIBLE);
        holder.playerOverlayController.setPlayState(playStateEvent);
        setLoadingState(holder, playStateEvent, isCurrentItem);

        if (isCurrentItem) {
            if (holder.isUIState(UIState.INITIAL) && playStateEvent.isPlayerPlaying()) {
                setInactiveUI(holder, adPage.getContext());
            } else if (holder.isUIState(UIState.PAUSED) && playStateEvent.playSessionIsActive()) {
                setInactiveUI(holder, adPage.getContext());
            } else if (!holder.isUIState(UIState.INITIAL) && !playStateEvent.playSessionIsActive()) {
                setPausedUI(holder);
            }
        }
    }

    @Override
    public void updatePlayQueueButton(View view) {
        // no-op
    }

    private void setLoadingState(Holder holder, PlayStateEvent playStateEvent, boolean isCurrentItem) {
        if (isCurrentItem) {
            holder.videoProgress.setVisibility(playStateEvent.isBuffering() && playStateEvent.playSessionIsActive() ?
                                               View.VISIBLE :
                                               View.GONE);
            if (playStateEvent.isPlayerPlaying() && !isVideoSurfaceVisible(holder)) {
                holder.videoTextureView.setVisibility(View.VISIBLE);
            }
        } else {
            holder.videoProgress.setVisibility(playStateEvent.playSessionIsActive() ? View.VISIBLE : View.GONE);
        }
    }

    private boolean isVideoSurfaceVisible(Holder holder) {
        return holder.videoTextureView.getVisibility() == View.VISIBLE;
    }

    private void setupLoadingStateViews(Holder holder, boolean isLetterboxVideo, boolean videoAlreadyStarted) {
        final boolean playControlsVisible = holder.playControlsHolder.getVisibility() == View.VISIBLE;
        holder.videoProgress.setVisibility(playControlsVisible || videoAlreadyStarted ? View.GONE : View.VISIBLE);
        holder.videoTextureView.setVisibility(videoAlreadyStarted ? View.VISIBLE : View.GONE);
        if (isLetterboxVideo) {
            holder.letterboxBackground.setVisibility(videoAlreadyStarted ? View.GONE : View.VISIBLE);
            holder.videoOverlayContainer.setVisibility(videoAlreadyStarted ? View.VISIBLE : View.GONE);
        }
    }

    private Holder getViewHolder(View videoPage) {
        return (Holder) videoPage.getTag();
    }

    private Animation fadeOutAnimation(Context context) {
        final Animation animation = AnimationUtils.loadAnimation(context, R.anim.abc_fade_out);
        animation.setStartOffset(FADE_OUT_OFFSET_MS);
        animation.setDuration(FADE_OUT_DURATION_MS);
        animation.setInterpolator(new AccelerateInterpolator(2.0f));
        return animation;
    }

    private void setInitialUI(Holder holder) {
        setVisibility(true, holder.fadingViews);
        holder.setUIState(UIState.INITIAL);
    }

    private void setInactiveUI(Holder holder, Context context) {
        setAnimation(holder.fadingViews, fadeOutAnimation(context));
        holder.setUIState(UIState.INACTIVE);
    }

    private void setPausedUI(Holder holder) {
        clearAnimation(holder.fadingViews);
        setVisibility(true, holder.fadingViews);
        holder.setUIState(UIState.PAUSED);
    }

    static class Holder extends AdHolder {

        private final View videoContainer;
        private final TextureView videoTextureView;
        private final View videoOverlayContainer;
        private final View videoOverlay;

        private final View fullscreenButton;
        private final View shrinkButton;

        private final View videoProgress;
        private final View letterboxBackground;

        private final PlayerOverlayController playerOverlayController;

        private final Iterable<View> onClickViews;
        Iterable<View> fadingViews = Collections.emptyList();

        private UIState currentUIState = UIState.INITIAL;

        Holder(View adView, PlayerOverlayController.Factory playerOverlayControllerFactory) {
            super(adView);
            videoContainer = adView.findViewById(R.id.video_container);
            videoTextureView = (TextureView) adView.findViewById(R.id.video_view);
            videoOverlayContainer = adView.findViewById(R.id.video_overlay_container);
            videoOverlay = adView.findViewById(R.id.video_overlay);

            fullscreenButton = adView.findViewById(R.id.video_fullscreen_control);
            shrinkButton = adView.findViewById(R.id.video_shrink_control);

            videoProgress = adView.findViewById(R.id.video_progress);
            letterboxBackground = adView.findViewById(R.id.letterbox_background);

            playerOverlayController = playerOverlayControllerFactory.create(videoOverlay);

            List<View> clickViews = Arrays.asList(playButton, nextButton, previousButton, shrinkButton,
                                                  fullscreenButton, videoOverlay, videoTextureView,
                                                  ctaButton, whyAds, skipAd);

            onClickViews = Iterables.filter(clickViews, presentInConfig);
        }

        boolean isUIState(UIState state) {
            return currentUIState == state;
        }

        void setUIState(UIState state) {
            currentUIState = state;
        }

        void setupFadingInterface(boolean enableAllFadeableElements) {
            final List<View> fadeViews = enableAllFadeableElements ?
                                         getAllFadeableElementViews() :
                                         Collections.singletonList(videoOverlayContainer);
            fadingViews = Iterables.filter(fadeViews, presentInConfig);
        }

        private List<View> getAllFadeableElementViews() {
            return Arrays.asList(whyAds, ctaButton, previewContainer, videoOverlayContainer);
        }
    }
}
