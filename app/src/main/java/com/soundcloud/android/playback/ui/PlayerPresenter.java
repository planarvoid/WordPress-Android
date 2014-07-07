package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;

import android.content.res.Resources;
import android.support.v4.view.ViewPager;

import javax.inject.Inject;

class PlayerPresenter {
    private final Resources resources;

    @Inject
    PlayerPresenter(Resources resources) {
        this.resources = resources;
    }

    public void initialize(ViewPager trackPager) {
        trackPager.setPageMargin(resources.getDimensionPixelSize(R.dimen.player_pager_spacing));
        trackPager.setPageMarginDrawable(R.color.black);
    }
}
