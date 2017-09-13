package com.soundcloud.android.cast;

import com.soundcloud.android.R;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlay;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayKey;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayPresenter;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import javax.inject.Inject;

public class CastIntroductoryOverlayPresenter extends DefaultActivityLightCycle<AppCompatActivity> {

    private final IntroductoryOverlayPresenter introductoryOverlayPresenter;

    private Toolbar toolBar;

    @Inject
    public CastIntroductoryOverlayPresenter(IntroductoryOverlayPresenter introductoryOverlayPresenter) {
        this.introductoryOverlayPresenter = introductoryOverlayPresenter;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        toolBar = activity.findViewById(R.id.toolbar_id);
    }

    public void showIntroductoryOverlayForCastIfNeeded() {
        findMenuItem(toolBar, R.id.media_route_menu_item).ifPresent(menuItem -> introductoryOverlayPresenter.showIfNeeded(IntroductoryOverlay.builder()
                                                                                                                                             .overlayKey(IntroductoryOverlayKey.CHROMECAST)
                                                                                                                                             .targetView(menuItem.getActionView())
                                                                                                                                             .title(R.string.cast_introductory_overlay_title)
                                                                                                                                             .description(R.string.cast_introductory_overlay_description)
                                                                                                                                             .build()));
    }

    private Optional<MenuItem> findMenuItem(Toolbar toolbar, @IdRes int menuItemIdRes) {
        if (toolbar != null && toolbar.getMenu() != null) {
            return Optional.fromNullable(toolbar.getMenu().findItem(menuItemIdRes));
        } else {
            return Optional.absent();
        }
    }
}
