package com.soundcloud.android.ads;

import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.LeaveBehindEvent;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.propeller.PropertySet;

import android.view.View;

public class LeaveBehindPresenter extends AdOverlayPresenter {

    private EventBus eventBus;

    public LeaveBehindPresenter(View trackView, Listener listener, EventBus eventBus, ImageOperations imageOperations) {
        super(trackView, R.id.leave_behind, R.id.leave_behind_stub, R.id.leave_behind_image, R.id.leave_behind_header, listener, imageOperations);
        this.eventBus = eventBus;
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

    @Override
    public void setVisible() {
        super.setVisible();
        eventBus.publish(EventQueue.LEAVE_BEHIND, LeaveBehindEvent.shown());
    }

    @Override
    public void setInvisible() {
        super.setInvisible();
        eventBus.publish(EventQueue.LEAVE_BEHIND, LeaveBehindEvent.hidden());
    }
}
