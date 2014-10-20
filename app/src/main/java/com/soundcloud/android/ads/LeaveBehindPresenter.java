package com.soundcloud.android.ads;

import com.soundcloud.android.R;
import com.soundcloud.propeller.PropertySet;

import android.view.View;

public class LeaveBehindPresenter extends AdOverlayPresenter {

    public LeaveBehindPresenter(View trackView, Listener listener) {
        super(trackView, R.id.leave_behind, R.id.leave_behind_stub, R.id.leave_behind_image, R.id.leave_behind_close, listener);
    }

    @Override
    public boolean shouldDisplayOverlay(PropertySet data, boolean isExpanded, boolean isPortrait, boolean isForeground) {
        final boolean adCompleteButNotClicked = data.getOrElse(LeaveBehindProperty.META_AD_COMPLETED, false)
                && !data.getOrElse(LeaveBehindProperty.META_AD_CLICKED, false);
        return isPortrait && adCompleteButNotClicked;
    }
}
