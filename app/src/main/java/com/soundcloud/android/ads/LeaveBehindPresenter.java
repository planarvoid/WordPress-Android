package com.soundcloud.android.ads;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.rx.eventbus.EventBus;

import android.view.View;

public class LeaveBehindPresenter extends AdOverlayPresenter {

    public LeaveBehindPresenter(View trackView, Listener listener, EventBus eventBus, ImageOperations imageOperations) {
        super(trackView, R.id.leave_behind, R.id.leave_behind_stub, R.id.leave_behind_image, R.id.leave_behind_image_holder, R.id.leave_behind_header, listener, imageOperations, eventBus);
    }

    @Override
    public boolean shouldDisplayOverlay(OverlayAdData data, boolean isExpanded, boolean isPortrait, boolean isForeground) {
        final boolean adCompleteButNotClicked = data.isMetaAdCompleted() && !data.isMetaAdClicked();
        return isPortrait && adCompleteButNotClicked;
    }

    @Override
    public boolean isFullScreen() {
        return false;
    }

}
