package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.Player;
import com.soundcloud.android.playback.mediaplayer.MediaPlayerVideoAdapter;
import com.soundcloud.java.collections.Iterables;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

class VideoAdPresenter extends AdPagePresenter implements View.OnClickListener {

    private final MediaPlayerVideoAdapter mediaPlayerVideoAdapter;
    private final ImageOperations imageOperations;
    private final AdPageListener listener;
    private final PlayerOverlayController.Factory playerOverlayControllerFactory;
    private final Resources resources;

    @Inject
    public VideoAdPresenter(MediaPlayerVideoAdapter mediaPlayerVideoAdapter, ImageOperations imageOperations,
                            AdPageListener listener, PlayerOverlayController.Factory playerOverlayControllerFactory,
                            Resources resources) {
        this.mediaPlayerVideoAdapter = mediaPlayerVideoAdapter;
        this.imageOperations = imageOperations;
        this.listener = listener;
        this.playerOverlayControllerFactory = playerOverlayControllerFactory;
        this.resources = resources;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.player_play:
            case R.id.video_view:
            case R.id.video_overlay:
            case R.id.video_pause_control:
                listener.onTogglePlay();
                break;
            case R.id.player_next:
                listener.onNext();
                break;
            case R.id.player_previous:
                listener.onPrevious();
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
        final View adView = LayoutInflater.from(container.getContext()).inflate(R.layout.player_ad_vertical_video_page, container, false);
        final Holder holder = new Holder(adView, playerOverlayControllerFactory);
        adView.setTag(holder);
        setVideoViewHolder(holder);
        resetSkipButton(holder, resources);
        return adView;
    }

    @Override
    public View clearItemView(View view) {
        return view;
    }

    @Override
    public void bindItemView(View view, PlayerAd playerAd) {
        final Holder holder = getViewHolder(view);
        resetSkipButton(holder, resources);
        displayPreview(playerAd, holder, imageOperations, resources);
        styleCallToActionButton(holder, playerAd, resources);
        setClickListener(this, holder.onClickViews);
    }

    public void setVideoViewHolder(Holder holder) {
        final SurfaceHolder surfaceHolder = holder.videoSurfaceView.getHolder();
        surfaceHolder.addCallback(mediaPlayerVideoAdapter);
    }

    @Override
    public void setProgress(View adView, PlaybackProgress progress) {
        updateSkipStatus(getViewHolder(adView), progress, resources);
    }

    @Override
    public void setPlayState(View adPage, Player.StateTransition stateTransition, boolean isCurrentItem, boolean isForeground) {
        final Holder holder = getViewHolder(adPage);
        final boolean playSessionIsActive = stateTransition.playSessionIsActive();
        holder.playControlsHolder.setVisibility(playSessionIsActive ? View.GONE : View.VISIBLE);
        holder.pauseButton.setVisibility(playSessionIsActive ? View.VISIBLE : View.GONE);
        holder.playerOverlayController.setPlayState(stateTransition);
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

    private Holder getViewHolder(View videoPage) {
        return (Holder) videoPage.getTag();
    }

    static class Holder extends AdHolder {

        private final SurfaceView videoSurfaceView;
        private final View videoOverlay;
        private final View pauseButton;

        private final PlayerOverlayController playerOverlayController;

        Iterable<View> onClickViews;

        Holder(View adView, PlayerOverlayController.Factory playerOverlayControllerFactory) {
            super(adView);

            videoSurfaceView = (SurfaceView) adView.findViewById(R.id.video_view);
            videoOverlay = adView.findViewById(R.id.video_overlay);
            pauseButton = adView.findViewById(R.id.video_pause_control);

            playerOverlayController = playerOverlayControllerFactory.create(videoOverlay);

            populateViewSets();
        }

        private void populateViewSets() {
            List<View> clickViews = Arrays.asList(playButton, nextButton, previousButton,
                    pauseButton, videoOverlay, videoSurfaceView, ctaButton, whyAds, skipAd);

            onClickViews = Iterables.filter(clickViews, presentInConfig);
        }
    }
}
