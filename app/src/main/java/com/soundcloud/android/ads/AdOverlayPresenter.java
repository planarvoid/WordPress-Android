package com.soundcloud.android.ads;

import com.soundcloud.android.events.AdOverlayEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImageListener;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.rx.eventbus.EventBus;

import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;

public abstract class AdOverlayPresenter {

    private final Listener listener;
    private final View overlay;
    private final ImageView adImage;
    private final View leaveBehindHeader;
    protected final ImageOperations imageOperations;
    protected final EventBus eventBus;

    private final ImageListener imageListener = new ImageListener() {

        @Override
        public void onLoadingStarted(String imageUri, View view) {
            // no-op
        }

        @Override
        public void onLoadingFailed(String imageUri, View view, String failedReason) {
            // no-op
        }

        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            listener.onAdImageLoaded();
        }
    };

    public abstract boolean shouldDisplayOverlay(OverlayAdData data,
                                                 boolean isExpanded,
                                                 boolean isPortrait,
                                                 boolean isForeground);

    public void onAdVisible(PlayQueueItem playQueueItem, OverlayAdData data, TrackSourceInfo trackSourceInfo) {
        overlay.setClickable(true);
        adImage.setVisibility(View.VISIBLE);
        leaveBehindHeader.setVisibility(View.VISIBLE);
        eventBus.publish(EventQueue.AD_OVERLAY, AdOverlayEvent.shown(playQueueItem.getUrn(), data, trackSourceInfo));
    }

    public void onAdNotVisible() {
        overlay.setClickable(false);
        adImage.setVisibility(View.GONE);
        leaveBehindHeader.setVisibility(View.GONE);
        eventBus.publish(EventQueue.AD_OVERLAY, AdOverlayEvent.hidden());
    }

    public boolean isNotVisible() {
        return adImage == null || adImage.getVisibility() == View.GONE;
    }

    public void clear() {
        adImage.setImageDrawable(null);
        onAdNotVisible();
    }

    public abstract boolean isFullScreen();

    public void bind(OverlayAdData data) {
        imageOperations.displayLeaveBehind(Uri.parse(data.getImageUrl()), getImageView(), imageListener);
    }

    public interface Listener {
        void onAdImageLoaded();

        void onImageClick();

        void onCloseButtonClick();
    }

    protected AdOverlayPresenter(View trackView,
                                 int overlayId,
                                 int overlayStubId,
                                 int adImageId,
                                 int adClickId,
                                 int headerId,
                                 final Listener listener,
                                 ImageOperations imageOperations,
                                 EventBus eventBus) {
        this.overlay = getOverlayView(trackView, overlayId, overlayStubId);
        this.listener = listener;
        this.eventBus = eventBus;

        this.adImage = (ImageView) overlay.findViewById(adImageId);
        final View adImageHolder = overlay.findViewById(adClickId);
        adImageHolder.setOnClickListener(v -> listener.onImageClick());

        this.leaveBehindHeader = overlay.findViewById(headerId);

        this.overlay.setOnClickListener(v -> listener.onCloseButtonClick());
        this.imageOperations = imageOperations;

    }

    private View getOverlayView(View trackView, int overlayId, int overlayStubId) {
        View overlayView = trackView.findViewById(overlayId);
        if (overlayView == null) {
            overlayView = ((ViewStub) trackView.findViewById(overlayStubId)).inflate();
        }
        return overlayView;
    }

    public ImageView getImageView() {
        return adImage;
    }

}
