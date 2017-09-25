package com.soundcloud.android.main;


import butterknife.BindView;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.events.GoOnboardingTooltipEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlay;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayKey;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayPresenter;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.ActivityLightCycleDispatcher;
import com.soundcloud.lightcycle.LightCycle;

import android.support.design.widget.AppBarLayout;
import android.support.v7.widget.Toolbar;
import android.view.View;

public abstract class MainNavigationView extends ActivityLightCycleDispatcher<RootActivity> implements EnterScreenDispatcher.Listener {

    @BindView(R.id.toolbar_id) Toolbar toolBar;
    @BindView(R.id.appbar) AppBarLayout appBarLayout;

    @LightCycle final EnterScreenDispatcher enterScreenDispatcher;

    final NavigationModel navigationModel;
    private final EventTracker eventTracker;
    private final IntroductoryOverlayPresenter introductoryOverlayPresenter;

    MainNavigationView(EnterScreenDispatcher enterScreenDispatcher,
                       NavigationModel navigationModel,
                       EventTracker eventTracker,
                       IntroductoryOverlayPresenter introductoryOverlayPresenter) {
        this.enterScreenDispatcher = enterScreenDispatcher;
        this.navigationModel = navigationModel;
        this.eventTracker = eventTracker;
        this.introductoryOverlayPresenter = introductoryOverlayPresenter;
        enterScreenDispatcher.setListener(this);
    }

    void setupViews(RootActivity activity, MainPagerAdapter pagerAdapter) {
        ButterKnife.bind(this, activity);
        activity.setSupportActionBar(toolBar);
        onSetupView(activity, pagerAdapter);
    }

    void showOfflineSettingsIntroductoryOverlay() {
        getMoreTabCustomView().ifPresent(view -> introductoryOverlayPresenter.showIfNeeded(IntroductoryOverlay.builder()
                                                                                                              .overlayKey(IntroductoryOverlayKey.OFFLINE_SETTINGS)
                                                                                                              .targetView(view)
                                                                                                              .title(R.string.overlay_offline_settings_title)
                                                                                                              .description(R.string.overlay_offline_settings_description)
                                                                                                              .event(Optional.of(GoOnboardingTooltipEvent.forOfflineSettings()))
                                                                                                              .build()));
    }

    protected abstract Optional<View> getMoreTabCustomView();

    @Override
    public void onReenterScreen(RootActivity activity) {
        toolBar.setTitle(getTitle(activity, currentTargetItem()));
        currentTargetItem().getPageViewScreen().ifPresent(screen -> eventTracker.trackScreen(ScreenEvent.create(screen), activity.getReferringEvent()));
    }

    @Override
    public void onEnterScreen(RootActivity activity, int position) {
        NavigationModel.Target target = navigationModel.getItem(position);
        toolBar.setTitle(getTitle(activity, target));
        target.getPageViewScreen().ifPresent(screen -> eventTracker.trackScreen(ScreenEvent.create(screen), activity.getReferringEvent()));
    }

    protected abstract void onSetupView(RootActivity activity, MainPagerAdapter pagerAdapter);

    protected abstract void onSelectItem(int position);

    protected abstract NavigationModel.Target currentTargetItem();

    protected String getTitle(RootActivity activity, NavigationModel.Target target) {
        return activity.getResources().getString(target.getName());
    }

    abstract void hideToolbar();

    abstract void showToolbar();

    void selectItem(Screen screen) {
        int position = navigationModel.getPosition(screen);
        if (position != NavigationModel.NOT_FOUND) {
            onSelectItem(position);
        }
    }

    Screen getScreen() {
        return currentTargetItem().getScreen();
    }
}
