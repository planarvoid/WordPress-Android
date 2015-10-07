package com.soundcloud.android.main;

import com.soundcloud.android.R;
import com.soundcloud.android.view.screen.BaseLayoutHelper;

import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import javax.inject.Inject;

public class MainTabsPresenter extends NavigationPresenter {

    private final BaseLayoutHelper layoutHelper;
    private final MainPagerAdapter.Factory pagerAdapterFactory;

    private AppCompatActivity activity;
    private MainPagerAdapter pagerAdapter;
    private ViewPager pager;

    @Inject
    MainTabsPresenter(BaseLayoutHelper layoutHelper, MainPagerAdapter.Factory pagerAdapterFactory) {
        this.layoutHelper = layoutHelper;
        this.pagerAdapterFactory = pagerAdapterFactory;
    }

    @Override
    public void setBaseLayout(AppCompatActivity activity) {
        layoutHelper.setBaseTabsLayout(activity);
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        super.onCreate(activity, bundle);
        this.activity = activity;
        pagerAdapter = pagerAdapterFactory.create(activity.getSupportFragmentManager());
        setupViews(activity);
    }

    private void setupViews(AppCompatActivity activity) {
        pager = (ViewPager) activity.findViewById(R.id.pager);
        bindPagerWithTabs(createTabs());
    }

    private void bindPagerWithTabs(TabLayout tabBar) {
        pager.setAdapter(pagerAdapter);
        tabBar.setOnTabSelectedListener(tabSelectedListener(pager, pagerAdapter));
        pager.addOnPageChangeListener(pageChangeListenerFor(tabBar));
        setTabIcons(pagerAdapter, tabBar, pager.getCurrentItem());
    }

    @NonNull
    private TabLayout createTabs() {
        TabLayout tabBar = new TabLayout(activity);
        tabBar.setTabGravity(TabLayout.GRAVITY_FILL);
        tabBar.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        addToToolbar(tabBar);
        return tabBar;
    }

    private void addToToolbar(TabLayout tabBar) {
        Toolbar toolbar = (Toolbar) activity.findViewById(R.id.toolbar_id);
        toolbar.setContentInsetsAbsolute(0, 0);
        toolbar.addView(tabBar);
    }

    private void setTabIcons(MainPagerAdapter pagerAdapter, TabLayout tabBar, int currentItem) {
        int tabCount = pagerAdapter.getCount();
        tabBar.removeAllTabs();
        for (int pageIndex = 0; pageIndex < tabCount; pageIndex++) {
            TabLayout.Tab tab = tabBar.newTab();
            tab.setCustomView(createTabViewFor(pagerAdapter.getPageIcon(pageIndex)));
            tab.setContentDescription(pagerAdapter.getPageTitle(pageIndex));
            tabBar.addTab(tab, pageIndex, pageIndex == currentItem);
        }
    }

    private View createTabViewFor(@DrawableRes int icon) {
        ImageView view = new ImageView(activity);
        view.setImageResource(icon);
        return view;
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

    private static ViewPager.OnPageChangeListener pageChangeListenerFor(final TabLayout tabBar) {
        /*
         * Workaround for tab re-selection callback issue:
         * https://code.google.com/p/android/issues/detail?id=177189#c16
         */
        return new TabLayout.TabLayoutOnPageChangeListener(tabBar) {
            @Override
            public void onPageSelected(int position) {
                if (tabBar.getSelectedTabPosition() != position) {
                    super.onPageSelected(position);
                }
            }
        };
    }

    @Override
    public void trackScreen() {
        // TODO: Track screens based on current tab selection
    }

}
