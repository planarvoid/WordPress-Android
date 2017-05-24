package com.soundcloud.android.ads;

import static com.soundcloud.android.utils.ViewUtils.forEach;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.android.view.CircularProgressBar;

import android.content.res.Resources;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;

class SponsoredSessionVideoView extends PrestitialView {

    @BindView(R.id.why_ads) TextView whyAds;

    @BindView(R.id.video_view) TextureView videoView;
    @BindView(R.id.video_progress) CircularProgressBar loadingIndicator;
    @BindView(R.id.viewability_layer) View viewabilityLayer;

    @BindView(R.id.video_container) View videoContainer;
    @BindView(R.id.video_overlay_container) View videoOverlayContainer;
    @BindView(R.id.video_overlay) View videoOverlay;

    @BindView(R.id.letterbox_background) View letterboxBackground;

    @BindView(R.id.player_play) View playButton;
    @BindView(R.id.player_next) View nextButton;
    @BindView(R.id.player_previous) View previousButton;
    @BindView(R.id.play_controls) View playControlsHolder;

    @BindView(R.id.skip_ad) View skipAd;
    @BindView(R.id.time_until_skip) View timeUntilSkip;
    @BindView(R.id.advertisement) View advertisement;

    private final Resources resources;
    private final AdStateProvider adStateProvider;
    private Iterable<View> fadingViews = Collections.emptyList();

    @Inject
    SponsoredSessionVideoView(Resources resources, AdStateProvider adStateProvider) {
        this.resources = resources;
        this.adStateProvider = adStateProvider;
    }

    public void setupContentView(View view, SponsoredSessionAd ad, Listener listener) {
        ButterKnife.bind(this, view);

        ViewUtils.setGone(Arrays.asList(nextButton, previousButton, playButton, videoOverlay));
        bindClickListeners(listener);
        bindVideoTextureView(ad, listener);
        // TODO: setupSkipButton(holder, playerAd);
        fadingViews = ad.video().isVerticalVideo() ? Arrays.asList(whyAds, advertisement) : fadingViews;
        adStateProvider.get(ad.video().uuid()).ifPresent(transition -> setPlayState(transition.stateTransition()));
    }

    private void bindVideoTextureView(SponsoredSessionAd ad, Listener listener) {
        listener.onVideoTextureBind(videoView, viewabilityLayer, ad.video());
    }

    private void bindClickListeners(Listener listener) {
        playButton.setOnClickListener(ignored -> listener.onTogglePlayback());
        videoContainer.setOnClickListener(ignored -> listener.onTogglePlayback());
        videoOverlay.setOnClickListener(ignored -> listener.onTogglePlayback());
        whyAds.setOnClickListener(textView -> listener.onWhyAdsClicked(textView.getContext()));
    }

    void adjustLayoutForVideo(VideoAd ad) {
        final LayoutParams layoutParams = adjustedVideoLayoutParams(ad);

        videoView.setLayoutParams(layoutParams);
        viewabilityLayer.setLayoutParams(layoutParams);
        videoOverlayContainer.setLayoutParams(layoutParams);
        letterboxBackground.setLayoutParams(layoutParams);

        letterboxBackground.setVisibility(ad.isVerticalVideo() ? View.GONE : View.VISIBLE);
        // TODO: holder.setupFadingInterface(ad.isVerticalVideo());
    }

    private LayoutParams adjustedVideoLayoutParams(VideoAd ad) {
        final LayoutParams layoutParams = videoView.getLayoutParams();

        if (ad.isVerticalVideo()) { // Vertical video view should scale like ImageView's CENTER_CROP
            final int sourceWidth = ad.firstVideoSource().width();
            final int sourceHeight = ad.firstVideoSource().height();
            final float scaleFactor = centerCropScaleFactor(videoContainer, sourceWidth, sourceHeight);
            layoutParams.width = (int) ((float) sourceWidth * scaleFactor);
            layoutParams.height = (int) ((float) sourceHeight * scaleFactor);
        } else {
            final int horizontalPadding = 2 * ViewUtils.dpToPx(resources, 5);
            layoutParams.width = videoContainer.getWidth() - horizontalPadding;
            layoutParams.height = (int) ((float) layoutParams.width * ad.videoProportion());
        }

        return layoutParams;
    }

    // Scale the video while maintaining aspect ratio so that both dimensions (width and height)
    // of the view will be equal to or larger than the corresponding dimension of the player.
    private float centerCropScaleFactor(View containerView, float sourceWidth, float sourceHeight) {
        final int viewWidth = containerView.getWidth();
        final int viewHeight = containerView.getHeight();

        final boolean isSourceWidthDominant = sourceWidth * viewHeight > sourceHeight * viewWidth;
        return isSourceWidthDominant ? viewHeight / sourceHeight : viewWidth / sourceWidth;
    }

    public void setPlayState(PlaybackStateTransition stateTransition) {
        final boolean isPaused = stateTransition.isPaused();
        final boolean videoHadStarted = stateTransition.isPlayerPlaying() || isPaused;
        final boolean isVideoViewVisible = videoView.getVisibility() == View.VISIBLE;

        playButton.setVisibility(isPaused ? View.VISIBLE : View.GONE);
        videoOverlay.setVisibility(isPaused ? View.VISIBLE : View.GONE);
        loadingIndicator.setVisibility(stateTransition.isBuffering() ? View.VISIBLE : View.GONE);

        if (stateTransition.isPlayerPlaying()) {
            setInactiveUI();
        } else if (stateTransition.isPlayerIdle()) {
            setActiveUI();
        }

        if (!isVideoViewVisible && videoHadStarted) {
            videoView.setVisibility(View.VISIBLE);
        }
    }

    private void setInactiveUI() {
        forEach(fadingViews, view -> {
            view.setAnimation(AnimationUtils.loadAnimation(videoView.getContext(), R.anim.ak_delayed_fade_out));
            view.setVisibility(View.INVISIBLE);
        });
    }

    private void setActiveUI() {
        forEach(fadingViews, view -> {
            view.clearAnimation();
            view.setVisibility(View.VISIBLE);
        });
    }
}
