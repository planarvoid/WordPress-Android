package com.soundcloud.android.ads;

import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.propeller.PropertySet;

import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;

public abstract class AdOverlayPresenter {

    private final View overlay;
    private final ImageView adImage;
    private final View leaveBehindClose;

    public abstract boolean shouldDisplayOverlay(PropertySet data, boolean isExpanded, boolean isPortrait, boolean isForeground);

    public void setVisible() {
        overlay.setClickable(true);
        adImage.setVisibility(View.VISIBLE);
        leaveBehindClose.setVisibility(View.VISIBLE);
    }

    public void setInvisible() {
        overlay.setClickable(false);
        adImage.setVisibility(View.GONE);
        leaveBehindClose.setVisibility(View.GONE);
    }

    public boolean isNotVisible() {
        return adImage == null || adImage.getVisibility() == View.GONE;
    }

    public void clear() {
        adImage.setImageDrawable(null);
        setInvisible();
    }

    public abstract boolean isFullScreen();

    public static interface Listener {
        void onImageClick();
        void onCloseButtonClick();
    }

    public static AdOverlayPresenter create(PropertySet data, View trackView, Listener listener, EventBus eventBus) {
        if (isInterstitial(data)) {
            return new InterstitialPresenter(trackView, listener);
        } else {
            return new LeaveBehindPresenter(trackView, listener, eventBus);
        }
    }

    private static boolean isInterstitial(PropertySet data) {
        return data != null && data.contains(InterstitialProperty.INTERSTITIAL_URN);
    }

    protected AdOverlayPresenter(View trackView, int overlayId, int overlayStubId, int adImageId, int closeId, final Listener listener) {
        this.overlay = getOverlayView(trackView, overlayId, overlayStubId);

        this.adImage = (ImageView) overlay.findViewById(adImageId);
        this.adImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onImageClick();
            }
        });

        this.leaveBehindClose = overlay.findViewById(closeId);
        this.leaveBehindClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onCloseButtonClick();
            }
        });

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
