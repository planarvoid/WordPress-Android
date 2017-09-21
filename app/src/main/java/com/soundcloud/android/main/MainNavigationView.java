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
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.ActivityLightCycleDispatcher;
import com.soundcloud.lightcycle.LightCycle;

import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.View;

public abstract class MainNavigationView extends ActivityLightCycleDispatcher<RootActivity> implements EnterScreenDispatcher.Listener {

    @BindView(R.id.pager) ViewPager pager;
    @BindView(R.id.toolbar_id) Toolbar toolBar;
    @BindView(R.id.appbar) AppBarLayout appBarLayout;
    @BindView(R.id.collapsing_toolbar) CollapsingToolbarLayout collapsingToolbarLayout;

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

        pager.setPageMargin(ViewUtils.dpToPx(activity, 10));
        pager.setPageMarginDrawable(R.color.page_background);

        activity.setSupportActionBar(toolBar);

        pager.setAdapter(pagerAdapter);

        onSetupView(pagerAdapter);
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
    public void onResume(RootActivity activity) {
        super.onResume(activity);

        pager.addOnPageChangeListener(enterScreenDispatcher);
        setTitle();
    }

    @Override
    public void onPause(RootActivity activity) {
        pager.removeOnPageChangeListener(enterScreenDispatcher);
        super.onPause(activity);
    }

    @Override
    public void onEnterScreen(RootActivity activity) {
        setTitle();
        getPageViewScreen().ifPresent(screen -> eventTracker.trackScreen(ScreenEvent.create(screen), activity.getReferringEvent()));
    }

    protected abstract void onSetupView(MainPagerAdapter pagerAdapter);

    protected abstract void onSelectItem(int position);

    private void setTitle() {
        toolBar.setTitle(pager.getAdapter().getPageTitle(pager.getCurrentItem()));
    }

    void hideToolbar() {
        if (collapsingToolbarLayout != null) {
            collapsingToolbarLayout.setVisibility(View.GONE);
        }
    }

    void showToolbar() {
        if (collapsingToolbarLayout != null && appBarLayout != null) {
            appBarLayout.setExpanded(true);
            collapsingToolbarLayout.setVisibility(View.VISIBLE);
        }
    }

    void selectItem(Screen screen) {
        int position = navigationModel.getPosition(screen);
        if (position != NavigationModel.NOT_FOUND) {
            onSelectItem(position);
        }
    }

    private NavigationModel.Target currentTargetItem() {
        return navigationModel.getItem(pager.getCurrentItem());
    }

    private Optional<Screen> getPageViewScreen() {
        return currentTargetItem().getPageViewScreen();
    }

    Screen getScreen() {
        return currentTargetItem().getScreen();
    }
}
