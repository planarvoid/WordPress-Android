package com.soundcloud.android.ads;

import com.soundcloud.android.events.AdOverlayEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImageListener;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.propeller.PropertySet;

import android.content.res.Resources;
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
    private final EventBus eventBus;

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

    public abstract boolean shouldDisplayOverlay(PropertySet data, boolean isExpanded, boolean isPortrait, boolean isForeground);

    public void setVisible() {
        overlay.setClickable(true);
        adImage.setVisibility(View.VISIBLE);
        leaveBehindHeader.setVisibility(View.VISIBLE);
        eventBus.publish(EventQueue.AD_OVERLAY, AdOverlayEvent.shown());
    }

    public void setInvisible() {
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
        setInvisible();
    }

    public abstract boolean isFullScreen();

    public void bind(PropertySet data) {
        imageOperations.displayLeaveBehind(Uri.parse(data.get(LeaveBehindProperty.IMAGE_URL)), getImageView(), imageListener);
    }

    public static interface Listener {
        void onAdImageLoaded();
        void onImageClick();
        void onCloseButtonClick();
    }

    public static AdOverlayPresenter create(PropertySet data, View trackView, Listener listener, EventBus eventBus, Resources resources, ImageOperations imageOperations) {
        if (isInterstitial(data)) {
            return new InterstitialPresenter(trackView, listener, eventBus, imageOperations, resources);
        } else {
            return new LeaveBehindPresenter(trackView, listener, eventBus, imageOperations);
        }
    }

    private static boolean isInterstitial(PropertySet data) {
        return data != null && data.contains(InterstitialProperty.INTERSTITIAL_URN);
    }

    protected AdOverlayPresenter(View trackView, int overlayId, int overlayStubId, int adImageId, int adClickId, int headerId, final Listener listener, ImageOperations imageOperations, EventBus eventBus) {
        this.overlay = getOverlayView(trackView, overlayId, overlayStubId);
        this.listener = listener;
        this.eventBus = eventBus;

        this.adImage = (ImageView) overlay.findViewById(adImageId);
        final View adImageHolder = overlay.findViewById(adClickId);
        adImageHolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onImageClick();
            }
        });

        this.leaveBehindHeader = overlay.findViewById(headerId);

        this.overlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onCloseButtonClick();
            }
        });
        this.imageOperations = imageOperations;

    }

    private View getOverlayView(View trackView, int overlayId, int overlayStubId) {
        View overlayView = trackView.findViewById(overlayId);
        if (overlayView == null){
            overlayView = ((ViewStub) trackView.findViewById(overlayStubId)).inflate();
        }
        return overlayView;
    }

    public ImageView getImageView() {
        return adImage;
    }

}
