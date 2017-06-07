package com.soundcloud.android.main;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.lightcycle.ActivityLightCycleDispatcher;
import com.soundcloud.lightcycle.LightCycle;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;

import javax.inject.Inject;

public class MainTabsView extends ActivityLightCycleDispatcher<RootActivity> implements EnterScreenDispatcher.Listener{

    @BindView(R.id.pager) ViewPager pager;
    @BindView(R.id.tab_layout) TabLayout tabBar;
    @BindView(R.id.toolbar_id) Toolbar toolBar;
    @BindView(R.id.appbar) AppBarLayout appBarLayout;
    @BindView(R.id.collapsing_toolbar) CollapsingToolbarLayout collapsingToolbarLayout;

    @LightCycle final EnterScreenDispatcher enterScreenDispatcher;

    private final NavigationModel navigationModel;
    private final EventTracker eventTracker;

    private RootActivity activity;

    @Inject
    MainTabsView(EnterScreenDispatcher enterScreenDispatcher, NavigationModel navigationModel, EventTracker eventTracker) {
        this.enterScreenDispatcher = enterScreenDispatcher;
        this.navigationModel = navigationModel;
        this.eventTracker = eventTracker;
        enterScreenDispatcher.setListener(this);
    }

    @Override
    public void onCreate(RootActivity activity, @Nullable Bundle bundle) {
        super.onCreate(activity, bundle);

        this.activity = activity;
    }

    void setupViews(RootActivity activity, MainPagerAdapter pagerAdapter) {
        ButterKnife.bind(this, activity);

        pager.setPageMargin(ViewUtils.dpToPx(activity, 10));
        pager.setPageMarginDrawable(R.color.page_background);

        activity.setSupportActionBar(toolBar);

        bindPagerToTabs(pagerAdapter);
    }

    private void bindPagerToTabs(MainPagerAdapter pagerAdapter) {
        pager.setAdapter(pagerAdapter);
        tabBar.setOnTabSelectedListener(tabSelectedListener(pager, pagerAdapter));
        pager.addOnPageChangeListener(pageChangeListenerFor(tabBar, pagerAdapter));
        setTabIcons(pagerAdapter, tabBar, pager.getCurrentItem());
    }


    private void setTabIcons(MainPagerAdapter pagerAdapter, TabLayout tabBar, int currentItem) {
        int tabCount = pagerAdapter.getCount();
        tabBar.removeAllTabs();
        for (int pageIndex = 0; pageIndex < tabCount; pageIndex++) {
            TabLayout.Tab tab = tabBar.newTab();
            tab.setCustomView(createTabViewFor(navigationModel.getItem(pageIndex)));
            tabBar.addTab(tab, pageIndex, pageIndex == currentItem);
        }
    }

    private View createTabViewFor(NavigationModel.Target target) {
        ImageView view = new ImageView(activity);
        view.setImageResource(target.getIcon());
        view.setContentDescription(activity.getString(target.getName()));
        return view;
    }

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
        eventTracker.trackScreen(ScreenEvent.create(getScreen()), activity.getReferringEvent());
    }

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
            tabBar.getTabAt(position).select();
        }
    }

    private NavigationModel.Target currentTargetItem() {
        return navigationModel.getItem(pager.getCurrentItem());
    }

    Screen getScreen() {
        return currentTargetItem().getScreen();
    }

    private static TabLayout.ViewPagerOnTabSelectedListener tabSelectedListener(final ViewPager pager,
                                                                                final MainPagerAdapter pagerAdapter) {
        return new TabLayout.ViewPagerOnTabSelectedListener(pager) {
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                pagerAdapter.resetScroll(tab.getPosition());
            }
        };
    }

    private static ViewPager.OnPageChangeListener pageChangeListenerFor(final TabLayout tabBar, final MainPagerAdapter pagerAdapter) {
        return new TabLayout.TabLayoutOnPageChangeListener(tabBar) {
            /*
            * Workaround for tab re-selection callback issue:
            * https://code.google.com/p/android/issues/detail?id=177189#c16
            */
            @Override
            public void onPageSelected(int position) {
                if (tabBar.getSelectedTabPosition() != position) {
                    super.onPageSelected(position);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                if (state == ViewPager.SCROLL_STATE_IDLE) {
                    pagerAdapter.setCurrentFragmentFocused();
                }
            }
        };
    }
}