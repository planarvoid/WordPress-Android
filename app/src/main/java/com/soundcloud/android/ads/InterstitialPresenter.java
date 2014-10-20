package com.soundcloud.android.ads;

import com.soundcloud.android.R;
import com.soundcloud.propeller.PropertySet;

import android.view.View;

public class InterstitialPresenter extends AdOverlayPresenter {

    public InterstitialPresenter(View trackView, Listener listener) {
        super(trackView, R.id.interstitial, R.id.interstitial_stub, R.id.interstitial_image, R.id.interstitial_close, listener);
    }

    @Override
    public boolean shouldDisplayOverlay(PropertySet data, boolean isExpanded, boolean isPortrait, boolean isForeground) {
        return isExpanded && isForeground;
    }
}
