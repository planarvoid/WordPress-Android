package com.soundcloud.android.ads;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.java.collections.PropertySet;

import android.view.View;

public class LeaveBehindPresenter extends AdOverlayPresenter {

    public LeaveBehindPresenter(View trackView, Listener listener, EventBus eventBus, ImageOperations imageOperations) {
        super(trackView, R.id.leave_behind, R.id.leave_behind_stub, R.id.leave_behind_image, R.id.leave_behind_image_holder, R.id.leave_behind_header, listener, imageOperations, eventBus);
    }

    @Override
    public boolean shouldDisplayOverlay(PropertySet data, boolean isExpanded, boolean isPortrait, boolean isForeground) {
        final boolean adCompleteButNotClicked = data.getOrElse(LeaveBehindProperty.META_AD_COMPLETED, false)
                && !data.getOrElse(LeaveBehindProperty.META_AD_CLICKED, false);
        return isPortrait && adCompleteButNotClicked;
    }

    @Override
    public boolean isFullScreen() {
        return false;
    }

}
