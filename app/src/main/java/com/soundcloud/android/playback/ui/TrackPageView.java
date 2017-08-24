package com.soundcloud.android.playback.ui;

import android.support.annotation.DrawableRes;
import android.widget.TextView;

public class TrackPageView {

    public TrackPageView() {
        super();
    }

    void showProBadge(TextView userView, boolean creatorIsPro, @DrawableRes int proBadge) {
        final int proBadgeDrawable = creatorIsPro ? proBadge : 0;
        userView.setCompoundDrawablesWithIntrinsicBounds(0, 0, proBadgeDrawable, 0);
    }
}
