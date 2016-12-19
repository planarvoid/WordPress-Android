package com.soundcloud.android.cast;

import com.soundcloud.android.R;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayKey;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayPresenter;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import javax.inject.Inject;

public class CastIntroductoryOverlayPresenter extends DefaultActivityLightCycle<AppCompatActivity> {

    private IntroductoryOverlayPresenter introductoryOverlayPresenter;
    private Resources resources;
    private Toolbar toolBar;

    @Inject
    public CastIntroductoryOverlayPresenter(IntroductoryOverlayPresenter introductoryOverlayPresenter,
                                            Resources resources) {
        this.introductoryOverlayPresenter = introductoryOverlayPresenter;
        this.resources = resources;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        toolBar = (Toolbar) activity.findViewById(R.id.toolbar_id);
    }

    public void showIntroductoryOverlayForCastIfNeeded() {
        introductoryOverlayPresenter.showForMenuItemIfNeeded(IntroductoryOverlayKey.CHROMECAST,
                                                             toolBar, R.id.media_route_menu_item,
                                                             resources.getString(R.string.cast_introductory_overlay_title),
                                                             resources.getString(R.string.cast_introductory_overlay_description));
    }
}
